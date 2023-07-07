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


class QueueIO[T <: Data : Real: BinaryRepresentation](proto: T) extends Bundle {
   val in = Flipped(DecoupledIO(proto.cloneType))
   val out = DecoupledIO(proto.cloneType)
}


class TLQueueBlock[T <: Data : Real: BinaryRepresentation](proto: T, address: AddressSet, beatBytes: Int = 4)(implicit p: Parameters) extends QueueBlock(proto, beatBytes) {
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

abstract class QueueBlock[T <: Data : Real: BinaryRepresentation](proto:T, beatBytes: Int) extends LazyModule()(Parameters.empty) with HasCSR {

    lazy val io = Wire(new QueueIO(proto))

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
case class QueueBlockParams[T <: Data : Real: BinaryRepresentation](
  proto: T,
  queueAddress: AddressSet
)

/* FixedPoint Queue Bellow */
case object QueueBlockFixedPointKey extends Field[Option[QueueBlockParams[FixedPoint]]](None)

trait CanHavePeripheryFixedPointQueueBlock { this: BaseSubsystem =>
  private val portName = "QueueBlock"

  val queue = p(QueueBlockFixedPointKey) match {
    case Some(params) => {
      val queue = LazyModule(new TLQueueBlock(proto = params.proto, address = params.queueAddress, beatBytes = pbus.beatBytes){
        def makeCustomIO(): QueueIO[FixedPoint] = {
          val io2: QueueIO[FixedPoint] = IO(io.cloneType)
          io2.suggestName("io")
          io2 <> io
          io2
        }
        val ioBlock = InModuleBody { makeCustomIO() }
      })
      pbus.coupleTo("queue") { queue.mem.get := TLFragmenter(pbus.beatBytes, pbus.blockBytes) := _ }
      Some(queue.ioBlock)
    }
    case None => None
  }
}

trait CanHavePeripheryFixedPointQueueBlockModuleImp extends LazyModuleImp {
    val outer: CanHavePeripheryFixedPointQueueBlock
    val queue  = outer.queue
}

class WithFixedPointQueueBlock(proto: FixedPoint = FixedPoint(16.W, 14.BP), queueAddress: AddressSet = AddressSet(0x2000, 0xff)) extends Config((site, here, up) => {
    case QueueBlockFixedPointKey => Some(
      QueueBlockParams(
        proto = proto,
        queueAddress = queueAddress
      )
    )
})

/* UInt Queue Bellow */
case object QueueBlockUIntKey extends Field[Option[QueueBlockParams[UInt]]](None)

trait CanHavePeripheryUIntQueueBlock { this: BaseSubsystem =>
  private val portName = "QueueBlock"
  val queueUInt = p(QueueBlockUIntKey) match {
    case Some(params) => {
      val queueUInt = LazyModule(new TLQueueBlock(proto = params.proto, address = params.queueAddress, beatBytes = pbus.beatBytes){
        def makeCustomIO(): QueueIO[UInt] = {
          val io2: QueueIO[UInt] = IO(io.cloneType)
          io2.suggestName("io")
          io2 <> io
          io2
        }
        val ioBlock = InModuleBody { makeCustomIO() }
      })
      pbus.coupleTo("queue") { queueUInt.mem.get := TLFragmenter(pbus.beatBytes, pbus.blockBytes) := _ }
      Some(queueUInt.ioBlock)
    }
    case None => None
  }
}

trait CanHavePeripheryUIntQueueBlockModuleImp extends LazyModuleImp {
    val outer: CanHavePeripheryUIntQueueBlock
    val queueUInt  = outer.queueUInt
}

class WithUIntQueueBlock(proto: UInt = UInt(16.W), queueAddress: AddressSet = AddressSet(0x2000, 0xff)) extends Config((site, here, up) => {
    case QueueBlockUIntKey => Some(
      QueueBlockParams(
        proto = proto,
        queueAddress = queueAddress
      )
    )
})
