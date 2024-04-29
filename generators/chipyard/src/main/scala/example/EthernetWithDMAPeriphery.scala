package chipyard.example

import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem.BaseSubsystem
import org.chipsalliance.cde.config.{Config, Field}

case class EthernetWithDMAParams (
  beatBytes      : Int,
  ethernetAddress: AddressSet,
  readerParams   : dmaParams,
  writerParams   : dmaParams,
)

/* EthernetWithDMA Key */
case object EthernetWithDMAKey extends Field[Option[EthernetWithDMAParams]](None)

// DOC include start: EthernetWithDMA lazy trait
trait CanHavePeripheryEthernetWithDMA { this: BaseSubsystem =>
  private val portName = "EthernetWithDMA"
  val ethDMA: Option[EthernetWithDMA] = p(EthernetWithDMAKey) match {
    case Some(params) => {
      val ethDMA = LazyModule(new EthernetWithDMA(params.beatBytes, params.ethernetAddress, params.readerParams, params.writerParams))
      // Connect Control registers
      pbus.coupleTo(portName) { ethDMA.mem.get := _ }
      // TL read and write nodes
      sbus.coupleFrom("dma_read")  { _ := ethDMA.readNode  }
      sbus.coupleFrom("dma_write") { _ := ethDMA.writeNode }
      // Interrupts
      ibus.fromSync := ethDMA.reader.io_interrupt
      ibus.fromSync := ethDMA.writer.io_interrupt
      // Return
      Some(ethDMA)
    }
    case None => None
  }
}
// DOC include end: EthernetWithDMA lazy trait

// DOC include start: EthernetWithDMA imp trait
trait CanHavePeripheryEthernetWithDMAModuleImp extends LazyRawModuleImp {
  override def provideImplicitClockToLazyChildren = true
  val outer: CanHavePeripheryEthernetWithDMA
}
// DOC include end: EthernetWithDMA imp trait

/* Mixin to add EthernetWithDMA to rocket config */
class WithEthernetWithDMA(params: EthernetWithDMAParams) extends Config((site, here, up) => { case EthernetWithDMAKey => Some(params) })