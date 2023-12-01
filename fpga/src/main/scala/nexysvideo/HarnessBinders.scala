// See LICENSE for license details.
package chipyard.fpga.nexysvideo

import chisel3._
import freechips.rocketchip.subsystem.PeripheryBusKey
import freechips.rocketchip.tilelink.TLBundle
import freechips.rocketchip.util.HeterogeneousBag
import freechips.rocketchip.diplomacy.LazyRawModuleImp
import sifive.blocks.devices.uart.UARTParams
import chipyard._
import chipyard.harness._
import testchipip._
import chipyard.iobinders._
import dspblocks.testchain._

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
  case (th: HasHarnessInstantiators, port: DSPChainPort) => {
    val nexysvideoth = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[NexysVideoHarness]
    port.io.data.pins.zip(nexysvideoth.io_lvds.get.bundle.lvds).foreach{ case (data, lvds) =>
      data.i_clock := lvds.o_clock
      data.i_reset := lvds.o_reset
      data.i_frame := lvds.o_frame
      data.i_valid := lvds.o_valid
      (data.i_data).zip(lvds.o_data).foreach({ case (i, o) => i := o })
    }

    port.io.eth.clk125 := nexysvideoth.ethClock_125.get.in.head._1.clock
    port.io.eth.clk125_90 := nexysvideoth.ethClock_125_90.get.in.head._1.clock
    port.io.eth.clk5 := nexysvideoth.ethClock_5.get.in.head._1.clock
    nexysvideoth.io_eth.get.bundle.phy_resetn := port.io.eth.phy_resetn
    nexysvideoth.io_eth.get.bundle.rgmii_txd := port.io.eth.rgmii_txd
    nexysvideoth.io_eth.get.bundle.rgmii_tx_ctl := port.io.eth.rgmii_tx_ctl
    nexysvideoth.io_eth.get.bundle.rgmii_txc := port.io.eth.rgmii_txc
    port.io.eth.rgmii_rxd := nexysvideoth.io_eth.get.bundle.rgmii_rxd
    port.io.eth.rgmii_rx_ctl := nexysvideoth.io_eth.get.bundle.rgmii_rx_ctl
    port.io.eth.rgmii_rxc := nexysvideoth.io_eth.get.bundle.rgmii_rxc
    port.io.eth.mdio <> nexysvideoth.ethOverlay.get.io.mdc
    nexysvideoth.io_eth.get.bundle.mdc := port.io.eth.mdc
  }
})

