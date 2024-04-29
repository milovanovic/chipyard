package rha

import DMAController._
import DMAController.DMAConfig.DMAConfig.{AXI, AXIL, AXIS}
import DMAController.DMAConfig._
import chisel3._
import dspblocks.datarx._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config.Parameters

case class dmaParams (
  reader   : readNodeParams,
  writer   : writeNodeParams,
  control  : controlNodeParams,
  dmaConfig: DMAConfig
)

class RHABundle(in2: DataIO) extends Bundle {
  val data: DataIO = in2.cloneType
}

class DataBundle(channels: Int) extends Bundle {
  val i_clock: Clock = Input(Clock())
  val i_reset: Bool = Input(Bool())
  val i_data: Vec[UInt] = Input(Vec(channels, UInt(8.W)))
  val i_valid: UInt = Input(UInt(8.W))
  val i_frame: UInt = Input(UInt(8.W))
}

class DataIO(channels: Int, chips: Int) extends Bundle {
  val pins: DataBundle = new DataBundle(channels)
}

trait HasRHAIO extends LazyModule {
  val io: RHABundle
}

trait DataRXIOs extends DataRX {
  private def makeCustomIO(): DataRXIO = {
    val io2: DataRXIO = IO(io.cloneType)
    io2.suggestName("io")
    io2 <> io
    io2
  }
  val ios: ModuleValue[DataRXIO] = InModuleBody { makeCustomIO() }
}

// RHA
class RHA(
  beatBytes:    Int,
  noOfChips:    Int,
  muxAddress:   AddressSet,
  datarxParams: DataRXParams,
  readerParams: dmaParams,
  writerParams: dmaParams,
) extends LazyModule()(Parameters.empty) with HasRHAIO {

  require(readerParams.dmaConfig.getBusConfig()._1 == AXI , "Reader side reader must be AXI")
  require(readerParams.dmaConfig.getBusConfig()._2 == AXIL, "Reader control must be AXI Lite")
  require(readerParams.dmaConfig.getBusConfig()._3 == AXIS, "Reader side writer must be AXIS")
  require(writerParams.dmaConfig.getBusConfig()._1 == AXIS, "Writer side reader must be AXIS")
  require(writerParams.dmaConfig.getBusConfig()._2 == AXIL, "Writer control must be AXI Lite")
  require(writerParams.dmaConfig.getBusConfig()._3 == AXI , "Writer side writer must be AXI")

  // Reader DMA
  val reader: DMAWrapper = LazyModule(new DMAWrapper(
    dmaConfig = readerParams.dmaConfig,
    readerParams = readerParams.reader,
    writerParams = readerParams.writer,
    controlParams = readerParams.control
  ))

  // Writer DMA
  val writer: DMAWrapper = LazyModule(new DMAWrapper(
    dmaConfig = writerParams.dmaConfig,
    readerParams = writerParams.reader,
    writerParams = writerParams.writer,
    controlParams = writerParams.control
  ))

  // DataRX
  private val datarx: DataRX with DataRXIOs = LazyModule(new DataRX(datarxParams) with DataRXIOs)

  // TLStreamMux
  private val mux: TLStreamMux = LazyModule(new TLStreamMux(muxAddress, beatBytes))

  // TL nodes
  val readNode: TLIdentityNode  = TLIdentityNode()
  val writeNode: TLIdentityNode = TLIdentityNode()

  // BUS TL node
  val bus: TLXbar = LazyModule(new TLXbar)
  val mem: Option[TLNexusNode] = Some(bus.node)

  // Connect read and write AXI4 nodes of DMA's
  (reader.io_read, writer.io_write) match {
    case (axir: AXI4MasterNode, axiw: AXI4MasterNode) => {
      readNode  := TLBuffer() := AXI4ToTL(wcorrupt = false) := AXI4UserYanker (capMaxFlight = Some(4096)) := AXI4Fragmenter() := AXI4IdIndexer (idBits = 1) := axir
      writeNode := TLBuffer() := AXI4ToTL(wcorrupt = false) := AXI4UserYanker (capMaxFlight = Some(4096)) := AXI4Fragmenter() := AXI4IdIndexer (idBits = 1) := axiw
    }
  }

  // Connect AXI4Stream nodes of DMA's
  (reader.io_write, writer.io_read) match {
    case (axism: AXI4StreamMasterNode, axiss: AXI4StreamSlaveNode) => {
      mux.streamNode := axism
      mux.streamNode := datarx.streamNode
      axiss := mux.streamNode
    }
  }

  // Connect Memory Ports to the Bus
  (reader.io_control, writer.io_control) match {
    case (axir: AXI4SlaveNode, axiw: AXI4SlaveNode) => {
      axir := AXI4UserYanker() := AXI4Deinterleaver(64) := TLToAXI4() := bus.node
      axiw := AXI4UserYanker() := AXI4Deinterleaver(64) := TLToAXI4() := bus.node
      mux.mem.get := bus.node
    }
  }

  /* IOs */
  lazy val io: RHABundle = IO(new RHABundle(new DataIO(datarxParams.channels, noOfChips)))

  lazy val module = new LazyModuleImp(this) {
    // We don't use Sync signals
    reader.io.sync.readerSync := 0.U
    reader.io.sync.writerSync := 0.U
    writer.io.sync.readerSync := 0.U
    writer.io.sync.writerSync := 0.U

    // Connect DataRX pins
    datarx.module.clock := io.data.pins.i_clock
    datarx.module.reset := io.data.pins.i_reset
    datarx.ios.i_valid  := io.data.pins.i_valid
    datarx.ios.i_frame  := io.data.pins.i_frame
    datarx.ios.i_data   := io.data.pins.i_data
    datarx.ios.i_dsp_clock.get := clock
    datarx.ios.i_dsp_reset.get := reset
  }
}

