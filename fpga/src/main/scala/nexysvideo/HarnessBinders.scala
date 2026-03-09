// See LICENSE for license details.
package chipyard.fpga.nexysvideo

import chisel3._
import freechips.rocketchip.util.ElaborationArtefacts
import org.chipsalliance.diplomacy.lazymodule.LazyRawModuleImp
import org.chipsalliance.diplomacy.nodes.HeterogeneousBag
import rivet.wrapper.rgmii_phy_io

//import sifive.fpgashells.ip.xilinx.{IBUFG, IOBUF, PULLUP, PowerOnResetFPGAOnly}
import chipyard.harness._
import chipyard.iobinders._
import sifive.fpgashells.shell._
import testchipip.serdes._

class WithNexysVideoUARTTSI(uartBaudRate: BigInt = 115200) extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: UARTTSIPort, chipId: Int) => {
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
  case (th: HasHarnessInstantiators, port: TLMemPort, chipId: Int) => {
    val nexysTh = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[NexysVideoHarness]
    val bundles = nexysTh.ddrClient.get.out.map(_._1)
    val ddrClientBundle = Wire(new HeterogeneousBag(bundles.map(_.cloneType)))
    bundles.zip(ddrClientBundle).foreach { case (bundle, io) => bundle <> io }
    ddrClientBundle <> port.io
  }
})

// Uses PMOD JA/JB
class WithNexysVideoSerialTLToGPIO extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: SerialTLPort, chipId: Int) => {
    val nexysTh = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[NexysVideoHarness]
    val harnessIO = IO(chiselTypeOf(port.io)).suggestName("serial_tl")
    harnessIO <> port.io
    
    harnessIO match {
      case io: DecoupledPhitIO => {
        val clkIO = io match {
          case io: HasClockOut => IOPin(io.clock_out)
          case io: HasClockIn => IOPin(io.clock_in)
        }
        val packagePinsWithPackageIOs = Seq(
          ("AB22", clkIO),
          ("AB21", IOPin(io.out.valid)),
          ("AB20", IOPin(io.out.ready)),
          ("AB18", IOPin(io.in.valid)),
          ("Y21", IOPin(io.in.ready)),
          ("AA21", IOPin(io.out.bits.phit, 0)),
          ("AA20", IOPin(io.out.bits.phit, 1)),
          ("AA18", IOPin(io.out.bits.phit, 2)),
          ("V9", IOPin(io.out.bits.phit, 3)),
          ("V8", IOPin(io.in.bits.phit, 0)),
          ("V7", IOPin(io.in.bits.phit, 1)),
          ("W7", IOPin(io.in.bits.phit, 2)),
          ("W9", IOPin(io.in.bits.phit, 3))
        )
        packagePinsWithPackageIOs foreach { case (pin, io) => {
          nexysTh.xdc.addPackagePin(io, pin)
          nexysTh.xdc.addIOStandard(io, "LVCMOS33")
        }}

        // Don't add IOB to the clock, if its an input
        io match {
          case io: DecoupledInternalSyncPhitIO => packagePinsWithPackageIOs foreach { case (pin, io) => {
            nexysTh.xdc.addIOB(io)
          }}
          case io: DecoupledExternalSyncPhitIO => packagePinsWithPackageIOs.drop(1).foreach { case (pin, io) => {
            nexysTh.xdc.addIOB(io)
          }}
        }

        nexysTh.sdc.addClock("ser_tl_clock", clkIO, 100)
        nexysTh.sdc.addGroup(pins = Seq(clkIO))
        nexysTh.xdc.clockDedicatedRouteFalse(clkIO)
      }
    }
  }
})

class WithNexysVideoEthernet extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: EthernetRGMIIPort, chipId: Int) =>
    val nexysTh = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[NexysVideoHarness]
    val harnessIO = IO(chiselTypeOf(port.io.phy)).suggestName("ethernet_io")
    harnessIO <> port.io.phy

    harnessIO match {
      case io: rgmii_phy_io =>
        port.io.gtx_clk := nexysTh.ethClock_125.get.in.head._1.clock
        port.io.gtx_clk90 := nexysTh.ethClock_125_90.get.in.head._1.clock
        port.io.gtx_rst := nexysTh.ethClock_125.get.in.head._1.reset.asBool
        val packagePinsWithPackageIOs = Seq(
          ("AB16", IOPin(io.rgmii_rxd(0))), // Sch=ETH_RXD0
          ("AA15", IOPin(io.rgmii_rxd(1))), // Sch=ETH_RXD1
          ("AB15", IOPin(io.rgmii_rxd(2))), // Sch=ETH_RXD2
          ("AB11", IOPin(io.rgmii_rxd(3))), // Sch=ETH_RXD3
          ("Y12",  IOPin(io.rgmii_txd(0))), // Sch=ETH_TXD0
          ("W12",  IOPin(io.rgmii_txd(1))), // Sch=ETH_TXD1
          ("W11",  IOPin(io.rgmii_txd(2))), // Sch=ETH_TXD2
          ("Y11",  IOPin(io.rgmii_txd(3))), // Sch=ETH_TXD3
          ("AA14", IOPin(io.rgmii_tx_clk)), // Sch=ETH_TXCK
          ("V10",  IOPin(io.rgmii_tx_ctl)), // Sch=ETH_TXCTL
          ("V13",  IOPin(io.rgmii_rx_clk)), // Sch=ETH_RXCK
          ("W10",  IOPin(io.rgmii_rx_ctl))  // Sch=ETH_RXCTL
        )
        packagePinsWithPackageIOs foreach { case (pin, io) =>
          nexysTh.xdc.addPackagePin(io, pin)
          nexysTh.xdc.addIOStandard(io, "LVCMOS25")
        }

        // Ethernet clock
        nexysTh.sdc.addClock("rgmii_rx_clk", IOPin(io.rgmii_rx_clk), 125)
        nexysTh.sdc.addGroup(clocks = Seq("rgmii_rx_clk"))

        nexysTh.xdc.addRawContent(
          "# Reset synchronization\n" +
          "set reset_ffs [get_cells -hier -regexp \".*/(rx|tx)_rst_reg_reg\\[\\\\d\\]\" " +
          "-filter {PARENT =~ *rgmii_phy_if_inst}]\n" +
          "set_property ASYNC_REG TRUE $reset_ffs\n" +
          "# Clock output ODDR\n" +
          "set_property ASYNC_REG TRUE " +
          "[get_cells -hierarchical -filter {NAME =~ *rgmii_phy_if_inst/clk_oddr_inst/oddr[0].oddr_inst}]"
        )

        nexysTh.sdc.addRawConstraint(
          "set_max_delay" +
          " -from [get_cells -hierarchical -filter {NAME =~ *rgmii_phy_if_inst/rgmii_tx_clk_1_reg}]" +
          " -to [get_cells -hierarchical -filter {NAME =~ *rgmii_phy_if_inst/clk_oddr_inst/oddr[0].oddr_inst}]" +
          " -datapath_only 2.000"
        )
        nexysTh.sdc.addRawConstraint(
          "set_max_delay" +
          " -from [get_cells -hierarchical -filter {NAME =~ *rgmii_phy_if_inst/rgmii_tx_clk_2_reg}]" +
          " -to [get_cells -hierarchical -filter {NAME =~ *rgmii_phy_if_inst/clk_oddr_inst/oddr[0].oddr_inst}]" +
          " -datapath_only 2.000"
        )
        nexysTh.sdc.addRawConstraint(
          "set_false_path -to [get_pins -of_objects $reset_ffs -filter {IS_PRESET || IS_RESET}]"
        )
  }
})


