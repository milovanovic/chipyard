package chipyard.example

import DMAController._
import DMAController.DMAConfig.DMAConfig.{AXI, AXIL, AXIS}
import DMAController.DMAConfig._
import chisel3._
import ethernet.{GbemacWrapperIO, TLGbemacWrapper}
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

trait HasEthernetWithDMAIO extends LazyModule {
  val io: GbemacWrapperIO
}

// EthernetWithDMA
class EthernetWithDMA(
           beatBytes      : Int,
           ethernetAddress: AddressSet,
           readerParams   : dmaParams,
           writerParams   : dmaParams,
         ) extends LazyModule()(Parameters.empty) with HasEthernetWithDMAIO {

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

  // Ethernet
  val ethernet: TLGbemacWrapper = LazyModule(new TLGbemacWrapper(ethernetAddress, beatBytes))

  // TL nodes
  val readNode : TLIdentityNode  = TLIdentityNode()
  val writeNode: TLIdentityNode = TLIdentityNode()

  // BUS TL node
  val bus: TLXbar = LazyModule(new TLXbar)
  val mem: Option[TLNexusNode] = Some(bus.node)

  // Connect read and write AXI4 nodes of DMA's
  (reader.io_read, writer.io_write) match {
    case (axir: AXI4MasterNode, axiw: AXI4MasterNode) => {
      readNode  := TLBuffer() := AXI4ToTL() := AXI4UserYanker (capMaxFlight = Some(4096)) := AXI4Fragmenter() := AXI4IdIndexer (idBits = 4) := axir
      writeNode := TLBuffer() := AXI4ToTL() := AXI4UserYanker (capMaxFlight = Some(4096)) := AXI4Fragmenter() := AXI4IdIndexer (idBits = 4) := axiw
    }
  }

  // Connect AXI4Stream nodes of DMA's to the Ethernet
  (reader.io_write, writer.io_read) match {
    case (axism: AXI4StreamMasterNode, axiss: AXI4StreamSlaveNode) => {
      ethernet.streamNode := axism
      axiss := ethernet.streamNode
    }
  }

  // Connect Memory Ports to the Bus
  (reader.io_control, writer.io_control) match {
    case (axir: AXI4SlaveNode, axiw: AXI4SlaveNode) => {
      axir := AXI4UserYanker() := AXI4Deinterleaver(64) := TLToAXI4() := bus.node
      axiw := AXI4UserYanker() := AXI4Deinterleaver(64) := TLToAXI4() := bus.node
      ethernet.mem.get := bus.node
    }
  }

  /* IOs */
  lazy val io: GbemacWrapperIO = ethernet.io

  lazy val module = new LazyModuleImp(this) {
    // We don't use Sync signals
    reader.io.sync.readerSync := 0.U
    reader.io.sync.writerSync := 0.U
    writer.io.sync.readerSync := 0.U
    writer.io.sync.writerSync := 0.U

  }
}

