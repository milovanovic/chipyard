package chain

import chisel3.{fromDoubleToLiteral => _, fromIntToBinaryPoint => _, _}
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config.{Config, Field}

import DMAController._
import DMAController.DMAConfig._

/* DMAEthernetChain Key */
case object DMAEthernetChainKey extends Field[Option[DMAEthernetChainParams]](None)

// DOC include start: DMAEthernetChain lazy trait
trait CanHavePeripheryDMAEthernetChain { this: BaseSubsystem =>
  private val portName = "chain"
  val chain = p(DMAEthernetChainKey) match {
    case Some(params) => {
      val chain = LazyModule(new DMAEthernet(params.dma, params.eth))
      // Connect Control registers
      pbus.coupleTo(portName) {
        chain.mem.get := _
      }
      chain.reader.io_read match {
        case axi: AXI4MasterNode => {
          // AXI4 read and write nodes
          sbus.coupleFrom("dma_read") {
            _ := TLBuffer() := AXI4ToTL(wcorrupt = false) := AXI4UserYanker (capMaxFlight = Some(4096)) := AXI4Fragmenter() := AXI4IdIndexer (idBits = 1) := axi
          }
        }
      }

      // Interrupts
      ibus.fromSync := chain.reader.io_interrupt
      // Return
      Some(chain)
    }
    case None => None
  }
}
// DOC include end: DMAEthernetChain lazy trait

// DOC include start: DMAEthernetChain imp trait
trait CanHavePeripheryDMAEthernetChainModuleImp extends LazyRawModuleImp {
  override def provideImplicitClockToLazyChildren = true
  val outer: CanHavePeripheryDMAEthernetChain
}
// DOC include end: DMAEthernetChain imp trait

/* Mixin to add DMAEthernetChain to rocket config */
class WithDMAEthernetChain(chainParams: DMAEthernetChainParams = DMAEthernetChainParams(DMAParams.readParams, DMAParams.ethParams))  extends Config((site, here, up) => { case DMAEthernetChainKey => Some(chainParams) })