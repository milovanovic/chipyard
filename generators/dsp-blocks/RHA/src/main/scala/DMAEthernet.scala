package chain

import DMAController._
import DMAController.DMAConfig.DMAConfig.{AXI, AXIL, AXIS}
import chisel3._
import dma.dmaParams
import ethernet.{GbemacWrapperIO, TLGbemacWrapper}
import freechips.rocketchip.amba.axi4.{AXI4IdIndexer, _}
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink.{TLNexusNode, TLToAXI4, TLXbar}
import org.chipsalliance.cde.config.Parameters

case class EthernetParams (
  val csrAddress: AddressSet,
  val beatBytes : Int
)

class DMAEthernet(
  dmaParameters: dmaParams,
  ethParameters: EthernetParams,
) extends LazyModule()(Parameters.empty) {

  require(dmaParameters.dmaConfig.getBusConfig()._1 == AXI , "Reader side reader must be AXI")
  require(dmaParameters.dmaConfig.getBusConfig()._2 == AXIL, "Reader control must be AXI Lite")
  require(dmaParameters.dmaConfig.getBusConfig()._3 == AXIS, "Reader side writer must be AXIS")

  // Ethernet IO
  lazy val io: GbemacWrapperIO = IO(new GbemacWrapperIO)

  // Ethernet
  val ethernet = LazyModule(new TLGbemacWrapper(ethParameters.csrAddress, ethParameters.beatBytes))

  // Reader DMA
  val reader: DMAWrapper = LazyModule(new DMAWrapper(
    dmaConfig = dmaParameters.dmaConfig,
    readerParams = dmaParameters.reader,
    writerParams = dmaParameters.writer,
    controlParams = dmaParameters.control
  ))

  // BUS
  val bus: TLXbar = LazyModule(new TLXbar)
  val mem: Option[TLNexusNode] = Some(bus.node)

  (reader.io_write) match {
    case (axism: AXI4StreamMasterNode) => {
      ethernet.streamNode.get := axism
    }
  }

  (reader.io_control) match {
    case (axir: AXI4SlaveNode) => {
      axir := AXI4UserYanker() := AXI4Deinterleaver(64) := TLToAXI4() := bus.node
      ethernet.mem.get := bus.node
    }
  }

  lazy val module = new LazyModuleImp(this) {
    reader.io.sync.readerSync := 0.U
    reader.io.sync.writerSync := 0.U

    ethernet.io.clk125        := io.clk125
    ethernet.io.clk125_90     := io.clk125_90
    ethernet.io.clk5          := io.clk5

    io.phy_resetn             := ethernet.io.phy_resetn
    io.rgmii_txd              := ethernet.io.rgmii_txd
    io.rgmii_tx_ctl           := ethernet.io.rgmii_tx_ctl
    io.rgmii_txc              := ethernet.io.rgmii_txc
    ethernet.io.rgmii_rxd     := io.rgmii_rxd
    ethernet.io.rgmii_rx_ctl  := io.rgmii_rx_ctl
    ethernet.io.rgmii_rxc     := io.rgmii_rxc
    ethernet.io.mdio          <> io.mdio
    io.mdc                    := ethernet.io.mdc
  }
}

