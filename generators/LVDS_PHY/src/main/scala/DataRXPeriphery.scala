// SPDX-License-Identifier: Apache-2.0

package lvdsphy

import chisel3.{Bundle, IO}
import freechips.rocketchip.amba.axi4stream.{AXI4StreamBundle, AXI4StreamSlaveParameters, AXI4StreamToBundleBridge}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem.BaseSubsystem
import org.chipsalliance.cde.config.{Config, Field}

/* DataRX Key */
case object DataRXKey extends Field[Option[DataRXParams]](None)

// DOC include start: DataRX lazy trait
trait CanHavePeripheryDataRX { this: BaseSubsystem =>
  private val portName = "datarx"
  val datarx: Option[DataRX with DataRXPins] = p(DataRXKey) match {
    case Some(params) => {
      val datarx = LazyModule(new DataRX(params) with DataRXPins)
      Some(datarx)
    }
    case None => None
  }
}
// DOC include end: DataRX lazy trait

// DOC include start: DataRX imp trait
trait CanHavePeripheryDataRXModuleImp extends LazyModuleImp {
  val outer: CanHavePeripheryDataRX
  val pins: Option[DataRXIO] = outer.datarx match {
    case Some(datarx) => {
      val pins = IO(datarx.ioBlock.cloneType)
      pins <> datarx.ioBlock
      Some(pins)
    }
    case None => None
  }
}
// DOC include end: DataRX imp trait

/* Mixin to add DataRX to rocket config */
class WithDataRX (datarxParams : DataRXParams = DataRXParams()) extends Config((site, here, up) => {
  case DataRXKey => Some(datarxParams)
})