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
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.regmapper._


class QueueIO extends Bundle {
   val in = Flipped(DecoupledIO(FixedPoint(16.W, 8.BP)))
   val out = DecoupledIO(FixedPoint(16.W, 8.BP))
}

class AXI4QueueBlock(address: AddressSet, beatBytes: Int = 4)(implicit p: Parameters) extends QueueBlock(beatBytes) {
  val mem = Some(AXI4RegisterNode(address = address, beatBytes = beatBytes))
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

trait AXI4QueueBlockStandaloneBlock extends AXI4QueueBlock{
    def beatBytes: Int = 4
    def standaloneParams = AXI4BundleParameters(addrBits = beatBytes*8, dataBits = beatBytes*8, idBits = 1)
    val ioMem = mem.map { m =>
        {
        val ioMemNode = BundleBridgeSource(() => AXI4Bundle(standaloneParams))
        m :=
            BundleBridgeToAXI4(AXI4MasterPortParameters(Seq(AXI4MasterParameters("bundleBridgeToAXI4")))) :=
            ioMemNode
        val ioMem = InModuleBody { ioMemNode.makeIO() }
        ioMem
        }
    }

    def makeCustomIO(): QueueIO = {
        val io2: QueueIO = IO(io.cloneType)
        io2.suggestName("io")
        io2 <> io
        io2
    }
    val ioBlock = InModuleBody { makeCustomIO() }
}


/* LIS parameters and addresses */
case class AXI4QueueBlockAddress(queueAddress: AddressSet)

/* AXI4QueueBlock UInt Key */
case object AXI4QueueBlockKey extends Field[Option[AXI4QueueBlockAddress]](None)

trait CanHavePeripheryAXI4QueueBlock { this: BaseSubsystem =>
  private val portName = "AXI4QueueBlock"

  val queue = p(AXI4QueueBlockKey) match {
    case Some(params) => {
        val queue = LazyModule(new AXI4QueueBlock(address = params.queueAddress, beatBytes = pbus.beatBytes){
            def makeCustomIO(): QueueIO = {
                val io2: QueueIO = IO(io.cloneType)
                io2.suggestName("io")
                io2 <> io
                io2
            }
            val ioBlock = InModuleBody { makeCustomIO() }
        })
        // Connect mem
        pbus.coupleTo("queue") { queue.mem.get := AXI4Buffer() := TLToAXI4() := TLFragmenter(pbus.beatBytes, pbus.blockBytes, holdFirstDeny = true) := _ }
        // return
        Some(queue.ioBlock)
    }
    case None => None
  }
}

trait CanHavePeripheryAXI4QueueBlockModuleImp extends LazyModuleImp {
    val outer: CanHavePeripheryAXI4QueueBlock
    val queue  = outer.queue
}

/* Mixin to add AXI4LIS to rocket config */
class WithAXI4QueueBlock(queueAddress: AddressSet = AddressSet(0x2000, 0xff)) extends Config((site, here, up) => {
    case AXI4QueueBlockKey => Some((AXI4QueueBlockAddress(queueAddress = queueAddress)))
})


case object AXI4QueueBlockAdapter {
  def tieoff(queue: Option[QueueIO]): Unit = {
    queue.foreach { s =>
      s.in.valid := true.B
      s.in.bits := 0x11112222.U
      s.out.ready := true.B
    }
  }

  def tieoff(queue: QueueIO): Unit = { tieoff(Some(queue)) }
}

// App
object AXI4QueueBlockApp extends App
{
  implicit val p: Parameters = Parameters.empty

  val lazyDut = LazyModule(new AXI4QueueBlock(AddressSet(0x2000, 0xF), 4) with AXI4QueueBlockStandaloneBlock)

  (new ChiselStage).execute(Array("--target-dir", "verilog/AXI4QueueBlock"), Seq(ChiselGeneratorAnnotation(() => lazyDut.module)))
}