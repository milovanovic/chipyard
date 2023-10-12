// SPDX-License-Identifier: Apache-2.0

package datarx

import chisel3.{Bundle, IO}
import freechips.rocketchip.amba.axi4stream.AXI4StreamBundle
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
  val ios: Option[DataRXBundle] = outer.datarx match {
    case Some(datarx) => {
      val ios = IO(datarx.ios.cloneType)
      ios <> datarx.ios
      Some(ios)
    }
    case None => None
  }
}
// DOC include end: DataRX imp trait

/* Mixin to add DataRX to rocket config */
class WithDataRX (datarxParams : DataRXParams = DataRXParams()) extends Config((site, here, up) => {
  case DataRXKey => Some(datarxParams)
})