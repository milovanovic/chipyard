package chipyard.fpga.nexysvideo

import chipyard._
import chipyard.harness._
import chisel3._
import freechips.rocketchip.amba.axi4stream.AXI4StreamBundle
import freechips.rocketchip.diplomacy.{LazyRawModuleImp, ModuleValue}
import freechips.rocketchip.tilelink.TLBundle
import freechips.rocketchip.util.HeterogeneousBag
import testchipip._
import lvdsphy._
import sifive.fpgashells.ip.xilinx.ILA_DATARX

class WithNexysVideoUARTTSI(uartBaudRate: BigInt = 115200) extends OverrideHarnessBinder({
  (system: CanHavePeripheryUARTTSI, th: HasHarnessInstantiators, ports: Seq[UARTTSIIO]) => {
    implicit val p = chipyard.iobinders.GetSystemParameters(system)
    require(ports.size <= 1)
    val nexysvideoth = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[NexysVideoHarness]
    ports.map({ port =>
      nexysvideoth.io_uart_bb.bundle <> port.uart
      nexysvideoth.other_leds(1) := port.dropped
      nexysvideoth.other_leds(2) := port.tsi2tl_state(0)
      nexysvideoth.other_leds(3) := port.tsi2tl_state(1)
      nexysvideoth.other_leds(4) := port.tsi2tl_state(2)
      nexysvideoth.other_leds(5) := port.tsi2tl_state(3)
    })
  }
})

class WithNexysVideoDDRTL extends OverrideHarnessBinder({
  (system: CanHaveMasterTLMemPort, th: HasHarnessInstantiators, ports: Seq[HeterogeneousBag[TLBundle]]) => {
    require(ports.size == 1)
    val nexysTh = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[NexysVideoHarness]
    val bundles = nexysTh.ddrClient.get.out.map(_._1)
    val ddrClientBundle = Wire(new HeterogeneousBag(bundles.map(_.cloneType)))
    bundles.zip(ddrClientBundle).foreach { case (bundle, io) => bundle <> io }
    ddrClientBundle <> ports.head
  }
})

class WithNexysVideoDataRX extends OverrideHarnessBinder({
  (system: CanHavePeripheryDataRXModuleImp, th: HasHarnessInstantiators, ports: Seq[DataRXIO]) => {
    implicit val p = chipyard.iobinders.GetSystemParameters(system)
    val nexysvideoth = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[NexysVideoHarness]
    ports.map({ port =>
      if (p(DataRXKey).get.asyncParams.isDefined) {
        port.i_dsp_clock.get := nexysvideoth.dutClock.in.head._1.clock
        port.i_dsp_reset.get := nexysvideoth.dutClock.in.head._1.reset
      }
      port.i_clock := nexysvideoth.io_lvds.get.bundle.o_clock
      port.i_reset := nexysvideoth.io_lvds.get.bundle.o_reset
      port.i_frame := nexysvideoth.io_lvds.get.bundle.o_frame
      port.i_valid := nexysvideoth.io_lvds.get.bundle.o_valid
      (port.i_data).zip(nexysvideoth.io_lvds.get.bundle.o_data).foreach({ case (i, o) => i := o })
      port.out.ready := 1.U

      val ila = Module(new ILA_DATARX)
      ila.io.clk := nexysvideoth.dutClock.in.head._1.clock
      ila.io.probe0 := port.out.bits(15,0)
      ila.io.probe1 := port.out.bits(31,15)
      ila.io.probe2 := port.out.bits(47,32)
      ila.io.probe3 := port.out.bits(63,48)
      ila.io.probe4 := port.out.valid
      ila.io.probe5 := nexysvideoth.io_lvds.get.bundle.o_reset
    })
  }
})