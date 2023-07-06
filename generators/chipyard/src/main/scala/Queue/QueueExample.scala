package chipyard.queue

import chisel3._
import chisel3.{Bundle, Module}
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chisel3.experimental.{DataMirror, FixedPoint}
import chisel3.util._
import dspblocks._
import dsptools.numbers._
import freechips.rocketchip.amba.axi4stream._
import org.chipsalliance.cde.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.regmapper._


class QueueIO extends Bundle {
   val in = Flipped(DecoupledIO(FixedPoint(16.W, 8.BP)))
   val out = DecoupledIO(FixedPoint(16.W, 8.BP))
}

class TLQueueBlock(address: AddressSet, beatBytes: Int = 4)(implicit p: Parameters) extends QueueBlock(beatBytes) {
  val devname = "TLQueueBlock"
  val devcompat = Seq("QueueBlock", "testBlock")
  val device = new SimpleDevice(devname, devcompat) {
    override def describe(resources: ResourceBindings): Description = {
      val Description(name, mapping) = super.describe(resources)
      Description(name, mapping)
    }
  }
  // make diplomatic TL node for regmap
  val mem = Some(TLRegisterNode(address = Seq(address), device = device, beatBytes = beatBytes))
  override def regmap(mapping: (Int, Seq[RegField])*): Unit = mem.get.regmap(mapping:_*)
}

abstract class QueueBlock(beatBytes: Int) extends LazyModule()(Parameters.empty) with HasCSR {

    lazy val io = Wire(new QueueIO)

    lazy val module = new LazyModuleImp(this) {

        // Define example register fields
        val enableReg = RegInit(true.B)
        val fields = Seq(RegField(1, enableReg,   RegFieldDesc(name = "control",    desc = "control")))
        regmap(fields.zipWithIndex.map({ case (f, i) => i * beatBytes -> Seq(f)}): _*)

        val queue = Module(new Queue(chiselTypeOf(io.out.bits), 2, pipe = true))

        queue.io.enq.valid := io.in.valid
        queue.io.enq.bits := io.in.bits
        io.in.ready := queue.io.enq.ready & enableReg

        queue.io.deq.ready := io.out.ready & enableReg
        io.out.valid := queue.io.deq.valid
        io.out.bits := queue.io.deq.bits
    }
}


/* QueueBlock parameters and addresses */
case class QueueBlockAddress(
  queueAddress: AddressSet
)

/* QueueBlock UInt Key */
case object QueueBlockKey extends Field[Option[QueueBlockAddress]](None)

trait CanHavePeripheryQueueBlock { this: BaseSubsystem =>
  private val portName = "QueueBlock"

  val queue = p(QueueBlockKey) match {
    case Some(params) => {
      val queue = LazyModule(new TLQueueBlock(address = params.queueAddress, beatBytes = pbus.beatBytes){
        def makeCustomIO(): QueueIO = {
          val io2: QueueIO = IO(io.cloneType)
          io2.suggestName("io")
          io2 <> io
          io2
        }
        val ioBlock = InModuleBody { makeCustomIO() }
      })
      // Connect mem
      pbus.coupleTo("queue") { queue.mem.get := TLFragmenter(pbus.beatBytes, pbus.blockBytes) := _ }
      // return
      Some(queue.ioBlock)
    }
    case None => None
  }
}

trait CanHavePeripheryQueueBlockModuleImp extends LazyModuleImp {
    val outer: CanHavePeripheryQueueBlock
    val queue  = outer.queue
}

/* Mixin to add QueueBlock to rocket config */
class WithQueueBlock(queueAddress: AddressSet = AddressSet(0x2000, 0xff)) extends Config((site, here, up) => {
    case QueueBlockKey => Some(
      QueueBlockAddress(
        queueAddress = queueAddress
      )
    )
})


case object QueueBlockAdapter {
  def tieoff(queue: Option[QueueIO]): Unit = {
    queue.foreach { s =>
      s.in.valid := true.B
      s.in.bits := 0x11112222.U
      s.out.ready := true.B
    }
  }

  def tieoff(queue: QueueIO): Unit = { tieoff(Some(queue)) }
}