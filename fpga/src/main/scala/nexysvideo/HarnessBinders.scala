// See LICENSE for license details.
package chipyard.fpga.nexysvideo

import chisel3._
import freechips.rocketchip.util.HeterogeneousBag
import freechips.rocketchip.diplomacy.LazyRawModuleImp

import chipyard.harness._
import chipyard.iobinders._


class WithNexysVideoUARTTSI(uartBaudRate: BigInt = 115200) extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: UARTTSIPort) => {
    val nexysvideoth = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[NexysVideoHarness]
    nexysvideoth.io_uart_bb.bundle <> port.io.uart
    nexysvideoth.other_leds(1) := port.io.dropped
    nexysvideoth.other_leds(2) := port.io.tsi2tl_state(0)
    nexysvideoth.other_leds(3) := port.io.tsi2tl_state(1)
    nexysvideoth.other_leds(4) := port.io.tsi2tl_state(2)
    nexysvideoth.other_leds(5) := port.io.tsi2tl_state(3)
  }
})

class WithNexysVideoDDRTL extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: TLMemPort) => {
    val nexysTh = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[NexysVideoHarness]
    val bundles = nexysTh.ddrClient.get.out.map(_._1)
    val ddrClientBundle = Wire(new HeterogeneousBag(bundles.map(_.cloneType)))
    bundles.zip(ddrClientBundle).foreach { case (bundle, io) => bundle <> io }
    ddrClientBundle <> port.io
  }
})


class WithNexysVideoDSPChain extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: DSPChainPort) =>
    val nexysvideoth = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[NexysVideoHarness]
    port.io.data.pins.zip(nexysvideoth.io_lvds.get.bundle.lvds).foreach{ case (data, lvds) =>
      data.i_clock := lvds.o_clock
      data.i_reset := lvds.o_reset
      data.i_frame := lvds.o_frame
      data.i_valid := lvds.o_valid
      data.i_data.zip(lvds.o_data).foreach({ case (i, o) => i := o })
    }

    port.io.eth.clk125 := nexysvideoth.ethClock_125.get.in.head._1.clock
    port.io.eth.clk125_90 := nexysvideoth.ethClock_125_90.get.in.head._1.clock
    port.io.eth.clk5 := nexysvideoth.ethClock_5.get.in.head._1.clock
    nexysvideoth.ethOverlay.get.io.phy_resetn := port.io.eth.phy_resetn
    nexysvideoth.ethOverlay.get.io.rgmii_txd.zipWithIndex.foreach({ case (m, i) => m := port.io.eth.rgmii_txd(i) })
    nexysvideoth.ethOverlay.get.io.rgmii_tx_ctl := port.io.eth.rgmii_tx_ctl
    nexysvideoth.ethOverlay.get.io.rgmii_txc := port.io.eth.rgmii_txc
    port.io.eth.rgmii_rxd := nexysvideoth.ethOverlay.get.io.rgmii_rxd.do_asUInt
    port.io.eth.rgmii_rx_ctl := nexysvideoth.ethOverlay.get.io.rgmii_rx_ctl
    port.io.eth.rgmii_rxc := nexysvideoth.ethOverlay.get.io.rgmii_rxc
    port.io.eth.mdio <> nexysvideoth.ethOverlay.get.io.mdio
    nexysvideoth.ethOverlay.get.io.mdc := port.io.eth.mdc
})

class WithNexysVideoTopLevel extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: TopLevelPort) => {
    val nexys = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[NexysVideoHarness]
    // Clock and reset
    port.io.clk_100MHZ := nexys.clockOverlay.overlayOutput.node.out.head._1.clock
    // Ethernet
    nexys.pinsOverlay.get.io.phy_resetn := port.io.phy_resetn
    nexys.pinsOverlay.get.io.rgmii_txd := port.io.rgmii_txd
    nexys.pinsOverlay.get.io.rgmii_tx_ctl := port.io.rgmii_tx_ctl
    nexys.pinsOverlay.get.io.rgmii_txc := port.io.rgmii_txc
    port.io.rgmii_rxd := nexys.pinsOverlay.get.io.rgmii_rxd
    port.io.rgmii_rx_ctl := nexys.pinsOverlay.get.io.rgmii_rx_ctl
    port.io.rgmii_rxc := nexys.pinsOverlay.get.io.rgmii_rxc
    port.io.mdio <> nexys.pinsOverlay.get.io.mdio
    nexys.pinsOverlay.get.io.mdc := port.io.mdc
    // LVDS 1
    port.io.io_2_lvds_clk_n := nexys.pinsOverlay.get.io.io_2_lvds_clk_n
    port.io.io_2_lvds_clk_p := nexys.pinsOverlay.get.io.io_2_lvds_clk_p
    port.io.io_2_lvds_data_p := nexys.pinsOverlay.get.io.io_2_lvds_data_p
    port.io.io_2_lvds_data_n := nexys.pinsOverlay.get.io.io_2_lvds_data_n
    port.io.io_2_lvds_valid_n := nexys.pinsOverlay.get.io.io_2_lvds_valid_n
    port.io.io_2_lvds_valid_p := nexys.pinsOverlay.get.io.io_2_lvds_valid_p
    port.io.io_2_lvds_frame_clk_n := nexys.pinsOverlay.get.io.io_2_lvds_frame_clk_n
    port.io.io_2_lvds_frame_clk_p := nexys.pinsOverlay.get.io.io_2_lvds_frame_clk_p
    // LVDS 2
    port.io.io_3_lvds_clk_n := nexys.pinsOverlay.get.io.io_3_lvds_clk_n
    port.io.io_3_lvds_clk_p := nexys.pinsOverlay.get.io.io_3_lvds_clk_p
    port.io.io_3_lvds_data_p := nexys.pinsOverlay.get.io.io_3_lvds_data_p
    port.io.io_3_lvds_data_n := nexys.pinsOverlay.get.io.io_3_lvds_data_n
    port.io.io_3_lvds_valid_n := nexys.pinsOverlay.get.io.io_3_lvds_valid_n
    port.io.io_3_lvds_valid_p := nexys.pinsOverlay.get.io.io_3_lvds_valid_p
    port.io.io_3_lvds_frame_clk_n := nexys.pinsOverlay.get.io.io_3_lvds_frame_clk_n
    port.io.io_3_lvds_frame_clk_p := nexys.pinsOverlay.get.io.io_3_lvds_frame_clk_p
    // CTRL 1
    port.io.awr_host_intr1_fmc := nexys.pinsOverlay.get.io.awr_host_intr1_fmc
    port.io.awr_host_intr2_fmc := nexys.pinsOverlay.get.io.awr_host_intr2_fmc
    port.io.awr_host_intr3_fmc := nexys.pinsOverlay.get.io.awr_host_intr3_fmc
    port.io.awr_host_intr4_fmc := nexys.pinsOverlay.get.io.awr_host_intr4_fmc
    nexys.pinsOverlay.get.io.awr_spi_cs1_fmc := port.io.awr_spi_cs1_fmc
    nexys.pinsOverlay.get.io.awr_spi_cs2_fmc := port.io.awr_spi_cs2_fmc
    nexys.pinsOverlay.get.io.awr_spi_cs3_fmc := port.io.awr_spi_cs3_fmc
    nexys.pinsOverlay.get.io.awr_spi_cs4_fmc := port.io.awr_spi_cs4_fmc
    port.io.awr_spi_miso_fmc := nexys.pinsOverlay.get.io.awr_spi_miso_fmc
    nexys.pinsOverlay.get.io.awr_spi_mosi_fmc := port.io.awr_spi_mosi_fmc
    nexys.pinsOverlay.get.io.awr_spi_clk_fmc := port.io.awr_spi_clk_fmc
    nexys.pinsOverlay.get.io.awr_nrst1_pmod := port.io.awr_nrst1_pmod
    nexys.pinsOverlay.get.io.awr_nrst2_fmc := port.io.awr_nrst2_fmc
    // CTRL 2
    nexys.pinsOverlay.get.io.awr_host_intr1 := port.io.awr_host_intr1
    nexys.pinsOverlay.get.io.awr_host_intr2 := port.io.awr_host_intr2
    nexys.pinsOverlay.get.io.awr_host_intr3 := port.io.awr_host_intr3
    nexys.pinsOverlay.get.io.awr_host_intr4 := port.io.awr_host_intr4
    port.io.awr_spi_cs1 := nexys.pinsOverlay.get.io.awr_spi_cs1
    port.io.awr_spi_cs2 := nexys.pinsOverlay.get.io.awr_spi_cs2
    port.io.awr_spi_cs3 := nexys.pinsOverlay.get.io.awr_spi_cs3
    port.io.awr_spi_cs4 := nexys.pinsOverlay.get.io.awr_spi_cs4
    nexys.pinsOverlay.get.io.awr_spi_miso := port.io.awr_spi_miso
    port.io.awr_spi_mosi := nexys.pinsOverlay.get.io.awr_spi_mosi
    port.io.awr_spi_clk := nexys.pinsOverlay.get.io.awr_spi_clk
    port.io.awr_nrst1 := nexys.pinsOverlay.get.io.awr_nrst1
    port.io.awr_nrst2 := nexys.pinsOverlay.get.io.awr_nrst2
  }
})
