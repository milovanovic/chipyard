package chipyard.fpga.zcu104

import freechips.rocketchip.devices.tilelink.BootROMLocated
import freechips.rocketchip.resources.DTSTimebase
import freechips.rocketchip.subsystem.{ExtMem, SystemBusKey}
import freechips.rocketchip.util.SystemFileName
import org.chipsalliance.cde.config.Config
import sifive.blocks.devices.spi.{PeripherySPIKey, SPIParams}
import sifive.blocks.devices.uart.{PeripheryUARTKey, UARTParams}
import sifive.fpgashells.shell.xilinx.{ZCU104DDRSize, ZCU104ShellPMOD}
import testchipip.serdes.SerialTLKey

import scala.sys.process._

class WithDefaultPeripherals extends Config((site, here, up) => {
  case PeripheryUARTKey => List(UARTParams(address = BigInt(0x64000000L)))
  case PeripherySPIKey => List(SPIParams(rAddress = BigInt(0x64001000L)))
  case ZCU104ShellPMOD => "SDIO"
})

class WithSystemModifications extends Config((site, here, up) => {
  case DTSTimebase => BigInt((1e6).toLong)
  case BootROMLocated(x) => up(BootROMLocated(x), site).map { p =>
    // invoke makefile for sdboot
    val freqMHz = (site(SystemBusKey).dtsFrequency.get / (1000 * 1000)).toLong
    val make = s"make -C fpga/src/main/resources/zcu104/sdboot PBUS_CLK=$freqMHz bin"
    require (make.! == 0, "Failed to build bootrom")
    p.copy(hang = 0x10000, contentFileName = SystemFileName(s"./fpga/src/main/resources/zcu104/sdboot/build/sdboot.bin"))
  }
  case ExtMem => up(ExtMem, site).map(x => x.copy(master = x.master.copy(size = site(ZCU104DDRSize)))) // set extmem to DDR size
  case SerialTLKey => Nil // remove serialized tl port
})

// DOC include start: AbstractZCU104 and Rocket
class WithZCU104Tweaks extends Config(
  // clocking
  new chipyard.harness.WithAllClocksFromHarnessClockInstantiator ++
  new chipyard.clocking.WithPassthroughClockGenerator ++
  new chipyard.config.WithUniformBusFrequencies(100) ++
  new WithFPGAFrequency(100) ++ // default 100MHz freq
  // harness binders
  new WithUART ++
  new WithSPISDCard ++
  new WithDDRMem ++
  new WithJTAG ++
  // other configuration
  new WithDefaultPeripherals ++
  new chipyard.config.WithTLBackingMemory ++ // use TL backing memory
  new WithSystemModifications ++ // setup busses, use sdboot bootrom, setup ext. mem. size
  new freechips.rocketchip.subsystem.WithoutTLMonitors ++
  new freechips.rocketchip.subsystem.WithNMemoryChannels(1)
)

class RocketZCU104Config extends Config(
  new WithZCU104Tweaks ++
  new chipyard.RocketConfig
)
// DOC include end: AbstractZCU104 and Rocket

class BoomZCU104Config extends Config(
  new WithFPGAFrequency(50) ++
  new WithZCU104Tweaks ++
  new chipyard.MegaBoomV3Config
)

class WithFPGAFrequency(fMHz: Double) extends Config(
  new chipyard.harness.WithHarnessBinderClockFreqMHz(fMHz) ++
  new chipyard.config.WithSystemBusFrequency(fMHz) ++
  new chipyard.config.WithPeripheryBusFrequency(fMHz) ++
  new chipyard.config.WithControlBusFrequency(fMHz) ++
  new chipyard.config.WithFrontBusFrequency(fMHz) ++
  new chipyard.config.WithMemoryBusFrequency(fMHz)
)

class WithFPGAFreq25MHz extends WithFPGAFrequency(25)
class WithFPGAFreq50MHz extends WithFPGAFrequency(50)
class WithFPGAFreq75MHz extends WithFPGAFrequency(75)
class WithFPGAFreq100MHz extends WithFPGAFrequency(100)
