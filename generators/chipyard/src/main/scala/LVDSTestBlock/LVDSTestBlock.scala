package chipyard.lvdsblock

import chisel3.{Bundle, _}
import chisel3.util.{Cat}
import dspblocks._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config.{Config, Field, Parameters}

class DiffPair extends Bundle {
  val p = Bool()
  val n = Bool()
}

class LVDSIO(val size: Int) extends Bundle {
  val in = Input(Vec(size, new DiffPair))
}


class TLLVDSBlock(size: Int, address: AddressSet, beatBytes: Int = 4)(implicit p: Parameters) extends LVDSBlock(size, beatBytes) {
  val devname = "TLLVDSBlock"
  val devcompat = Seq("LVDSBlock", "lvdsBlock")
  val device = new SimpleDevice(devname, devcompat) {
    override def describe(resources: ResourceBindings): Description = {
      val Description(name, mapping) = super.describe(resources)
      Description(name, mapping)
    }
  }
  // make diplomatic TL node for regmap
  val mem: Option[TLRegisterNode] = Some(TLRegisterNode(address = Seq(address), device = device, beatBytes = beatBytes))
  override def regmap(mapping: (Int, Seq[RegField])*): Unit = mem.get.regmap(mapping:_*)
}

abstract class LVDSBlock(val size: Int, val beatBytes: Int) extends LazyModule()(Parameters.empty) with HasCSR {

  lazy val io = Wire(new LVDSIO(size))

  lazy val module = new LazyModuleImp(this) {

    // Define example register fields
    val testReg = RegInit(0.U((2*size).W))
    val fields = Seq(RegField(testReg.getWidth, testReg, RegFieldDesc(name = "testReg", desc = "testReg")))
    regmap(fields.zipWithIndex.map({ case (f, i) => i * beatBytes -> Seq(f)}): _*)

    io.in.zipWithIndex.foreach({ case (m, i) => testReg(2*i+1, 2*i) := Cat(m.p, m.n) })
  }
}


/* LVDSBlock parameters and addresses */
case class LVDSBlockParams(
  size: Int,
  lvdsAddress: AddressSet
)

/* LVDS Bellow */
case object LVDSBlockKey extends Field[Option[LVDSBlockParams]](None)

trait CanHavePeripheryLVDSBlock { this: BaseSubsystem =>
  private val portName = "LVDSBlock"
  val lvds = p(LVDSBlockKey) match {
    case Some(params) => {
      val lvds = LazyModule(new TLLVDSBlock(size = params.size, address = params.lvdsAddress, beatBytes = pbus.beatBytes){
        def makeCustomIO(): LVDSIO = {
          val io2: LVDSIO = IO(io.cloneType)
          io2.suggestName("io")
          io2 <> io
          io2
        }
        val ioBlock = InModuleBody { makeCustomIO() }
      })
      pbus.coupleTo("lvds") { lvds.mem.get := TLFragmenter(pbus.beatBytes, pbus.blockBytes) := _ }
      Some(lvds.ioBlock)
    }
    case None => None
  }
}

trait CanHavePeripheryLVDSBlockModuleImp extends LazyModuleImp {
  val outer: CanHavePeripheryLVDSBlock
  val lvds  = outer.lvds
}

class WithUIntLVDSBlock(size: Int = 4, lvdsAddress: AddressSet = AddressSet(0x2000, 0xff)) extends Config((site, here, up) => {
  case LVDSBlockKey => Some(
    LVDSBlockParams(
      size = size,
      lvdsAddress = lvdsAddress
    )
  )
})
