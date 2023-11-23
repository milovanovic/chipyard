// See LICENSE for license details.
package chipyard.fpga.nexysvideo

import org.chipsalliance.cde.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.system._
import freechips.rocketchip.tile._

import sifive.blocks.devices.uart._
import sifive.fpgashells.shell.DesignKey


import testchipip.SerialTLKey

import chipyard.BuildSystem

import dspblocks.testchain._

// don't use FPGAShell's DesignKey
class WithNoDesignKey extends Config((site, here, up) => {
  case DesignKey => (p: Parameters) => new SimpleLazyModule()(p)
})

class SmallRocketConfig extends Config(
  new freechips.rocketchip.subsystem.WithNSmallCores(1) ++         // single rocket-core
  new chipyard.config.AbstractConfig)

class MedRocketConfig extends Config(
  new freechips.rocketchip.subsystem.WithNMedCores(1) ++         // single rocket-core
  new chipyard.config.AbstractConfig)

class SmallRV32RocketConfig extends Config(
  new freechips.rocketchip.subsystem.WithRV32 ++            // set RocketTiles to be 32-bit
  new freechips.rocketchip.subsystem.WithNSmallCores(1) ++
  new chipyard.config.AbstractConfig)

class MedRV32RocketConfig extends Config(
  new freechips.rocketchip.subsystem.WithRV32 ++            // set RocketTiles to be 32-bit
  new freechips.rocketchip.subsystem.WithNMedCores(1) ++
  new chipyard.config.AbstractConfig)

// DOC include start: WithNexysVideoTweaks and Rocket
class WithNexysVideoTweaks extends Config(
  new chipyard.iobinders.WithChainPunchthrough ++
  new WithNexysVideoUARTTSI ++
  new WithNexysVideoDDRTL ++
  new WithNoDesignKey ++
  new testchipip.WithUARTTSIClient ++
  new chipyard.harness.WithSerialTLTiedOff ++
  new chipyard.harness.WithHarnessBinderClockFreqMHz(50) ++
  new chipyard.config.WithMemoryBusFrequency(50.0) ++
  new chipyard.config.WithFrontBusFrequency(50.0) ++
  new chipyard.config.WithSystemBusFrequency(50.0) ++
  new chipyard.config.WithPeripheryBusFrequency(50.0) ++
  new chipyard.harness.WithAllClocksFromHarnessClockInstantiator ++
  new chipyard.clocking.WithPassthroughClockGenerator ++
  new chipyard.config.WithNoDebug ++ // no jtag
  new chipyard.config.WithNoUART ++ // use UART for the UART-TSI thing instad
  new chipyard.config.WithTLBackingMemory ++ // FPGA-shells converts the AXI to TL for us
  new freechips.rocketchip.subsystem.WithExtMemSize(BigInt(512) << 20) ++ // 512mb on Nexys Video
  new freechips.rocketchip.subsystem.WithoutTLMonitors)

class RocketNexysVideoConfig extends Config(
  new WithNexysVideoTweaks ++
  new WithTestChain(new TestChainParams(
    rxNum = 4, txNum = 4, angleFFTSizeAfterPadding = 32,
    rangeFFTSize = 256, dopplerFFTSize = 32, dopplerOutputNodes = 4, dopplerNumInAngleBranch = 4
  ).params) ++
  new WithNexysVideoDSPChain ++
  new chipyard.config.WithBroadcastManager ++ // no l2
  new chipyard.RocketConfig)
// DOC include end: WithNexysVideoTweaks and Rocket

class SmallRocketNexysVideoConfig extends Config(
  new WithNexysVideoTweaks ++
  new WithTestChain(new TestChainParams(
    rxNum = 4, txNum = 4, angleFFTSizeAfterPadding = 32,
    rangeFFTSize = 256, dopplerFFTSize = 32, dopplerOutputNodes = 4, dopplerNumInAngleBranch = 4
  ).params) ++
  new WithNexysVideoDSPChain ++
  new chipyard.config.WithBroadcastManager ++ // no l2
  new SmallRocketConfig)

class MedRocketNexysVideoConfig extends Config(
  new WithNexysVideoTweaks ++
  new WithTestChain(new TestChainParams(
    rxNum = 4, txNum = 4, angleFFTSizeAfterPadding = 32,
    rangeFFTSize = 256, dopplerFFTSize = 32, dopplerOutputNodes = 4, dopplerNumInAngleBranch = 4
  ).params) ++
  new WithNexysVideoDSPChain ++
  new chipyard.config.WithBroadcastManager ++ // no l2
  new MedRocketConfig)

class RV32RocketNexysVideoConfig extends Config(
  new WithNexysVideoTweaks ++
  new WithTestChain(new TestChainParams(
    rxNum = 4, txNum = 4, angleFFTSizeAfterPadding = 32,
    rangeFFTSize = 256, dopplerFFTSize = 32, dopplerOutputNodes = 4, dopplerNumInAngleBranch = 4
  ).params) ++
  new WithNexysVideoDSPChain ++
  new chipyard.config.WithBroadcastManager ++ // no l2
  new chipyard.RV32RocketConfig)

class SmallRV32RocketNexysVideoConfig extends Config(
  new WithNexysVideoTweaks ++
    new WithTestChain(new TestChainParams(
      rxNum = 4, txNum = 4, angleFFTSizeAfterPadding = 32,
      rangeFFTSize = 256, dopplerFFTSize = 32, dopplerOutputNodes = 4, dopplerNumInAngleBranch = 4
    ).params) ++
  new WithNexysVideoDSPChain ++
  new chipyard.config.WithBroadcastManager ++ // no l2
  new SmallRV32RocketConfig
)

class MedRV32RocketNexysVideoConfig extends Config(
  new WithNexysVideoTweaks ++
  new WithTestChain(new TestChainParams(
    rxNum = 4, txNum = 4, angleFFTSizeAfterPadding = 32,
    rangeFFTSize = 256, dopplerFFTSize = 32, dopplerOutputNodes = 4, dopplerNumInAngleBranch = 4
  ).params) ++
  new WithNexysVideoDSPChain ++
  new chipyard.config.WithBroadcastManager ++ // no l2
  new MedRV32RocketConfig
)

// DOC include start: WithTinyNexysVideoTweaks and Rocket
class WithTinyNexysVideoTweaks extends Config(
  new chipyard.iobinders.WithChainPunchthrough ++
  new WithNexysVideoUARTTSI ++
  new WithNoDesignKey ++
  new sifive.fpgashells.shell.xilinx.WithNoNexysVideoShellDDR ++ // no DDR
  new testchipip.WithUARTTSIClient ++
  new chipyard.harness.WithSerialTLTiedOff ++
  new chipyard.harness.WithHarnessBinderClockFreqMHz(50) ++
  new chipyard.config.WithMemoryBusFrequency(50.0) ++
  new chipyard.config.WithFrontBusFrequency(50.0) ++
  new chipyard.config.WithSystemBusFrequency(50.0) ++
  new chipyard.config.WithPeripheryBusFrequency(50.0) ++
  new chipyard.harness.WithAllClocksFromHarnessClockInstantiator ++
  new chipyard.clocking.WithPassthroughClockGenerator ++
  new chipyard.config.WithNoDebug ++ // no jtag
  new chipyard.config.WithNoUART ++ // use UART for the UART-TSI thing instad
  new freechips.rocketchip.subsystem.WithoutTLMonitors)

class TinyRocketNexysVideoConfig extends Config(
  new WithTinyNexysVideoTweaks ++
  new WithTestChain(new TestChainParams(
    rxNum = 4, txNum = 2, angleFFTSizeAfterPadding = 32,
    rangeFFTSize = 256, dopplerFFTSize = 32, dopplerOutputNodes = 4, dopplerNumInAngleBranch = 4
  ).params) ++
  new WithNexysVideoDSPChain ++
  new chipyard.config.WithBroadcastManager ++ // no l2
  new chipyard.TinyRocketConfig)
// DOC include end: WithTinyNexysVideoTweaks and Rocket