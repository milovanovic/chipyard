// SPDX-License-Identifier: Apache-2.0

package dspblocks.datarx

import chisel3._
import chisel3.util.ShiftRegister
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config.Parameters

// ByteAligment parameters
case class ByteAligmentParams(
  channels: Int = 6)

class ByteAligmentIO(val channels: Int) extends Bundle {
  val valid = Input(UInt(8.W))
  val in = Input(Vec(channels, UInt(8.W)))
  val out = Output(Vec(channels, UInt(8.W)))
  val en = Output(Bool())
}

class ByteAligment(val params: ByteAligmentParams) extends LazyModule()(Parameters.empty) {

  private val reorder: Seq[BitReordering with BitReorderingPins] = Seq.fill(params.channels) {
    LazyModule(new BitReordering with BitReorderingPins)
  }
  val bitslip: BitSlipDetection with BitSlipDetectionPins = LazyModule(new BitSlipDetection with BitSlipDetectionPins)

  lazy val io: ByteAligmentIO = Wire(new ByteAligmentIO(params.channels))

  lazy val module = new LazyModuleImp(this) {

    bitslip.ioBlock.i_data := io.valid
    io.en := ShiftRegister(bitslip.ioBlock.o_en, 3, 0.U, true.B)

    reorder.zipWithIndex.foreach({
      case (m, i) =>
        m.ioBlock.i_data := RegNext(io.in(i), 0.U)
        m.ioBlock.i_bitslip := bitslip.ioBlock.o_bitslip
        io.out(i) := m.ioBlock.o_data
    })
  }
}

trait ByteAligmentPins extends ByteAligment {

  def makeCustomIO() = {
    val io2 = IO(io.cloneType)
    io2.suggestName("io")
    io2 <> io
    io2
  }
  val ioBlock = InModuleBody { makeCustomIO() }
}

object ByteAligmentApp extends App {
  val lazyDut = LazyModule(new ByteAligment(ByteAligmentParams()) with ByteAligmentPins)
  (new ChiselStage)
    .execute(Array("--target-dir", "verilog/ByteAligment"), Seq(ChiselGeneratorAnnotation(() => lazyDut.module)))
}
