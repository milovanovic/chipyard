// SPDX-License-Identifier: Apache-2.0

package datarx

import chisel3._
import chisel3.util.{Cat, DecoupledIO, ShiftRegister}
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config.Parameters
import dsputils._

// DataRX parameters
case class DataRXParams(
  channels    : Int = 4,
  asyncParams : Option[AXI4StreamAsyncQueueWithControlParams] = Some(AXI4StreamAsyncQueueWithControlParams(
    ctrlBits = 3,
    sync = 4,
    depth = 16,
    safe = true
  ))){
  require(channels >= 1, "Number of channels must be greater or equal to one!")
}

class DataRXIO(channels: Int, async: Boolean) extends Bundle {
  val i_clock: Clock = Input(Clock())
  val i_reset: Bool = Input(Bool())
  val i_data: Vec[UInt] = Input(Vec(channels, UInt(8.W)))
  val i_valid: UInt = Input(UInt(8.W))
  val i_frame: UInt = Input(UInt(8.W))
  val o_crc: Bool = Output(Bool())
  val o_word_size: UInt = Output(UInt(2.W))
  // DSP clock and reset if Async FIFO enabled
  val i_dsp_clock: Option[Clock] = if (async) Some(Input(Clock())) else None
  val i_dsp_reset: Option[Bool] = if (async) Some(Input(Bool())) else None
  // output stream signals
  val out = DecoupledIO(UInt((channels*16).W))
}

object DataRXIO {
  def apply(channels: Int, async: Boolean): DataRXIO = new DataRXIO(channels, async)
}

trait DataRXPins extends DataRX{
  private def makeCustomIO(): DataRXIO = {
    val io2: DataRXIO = IO(io.cloneType)
    io2.suggestName("io")
    io2 <> io
    io2
  }
  val ioBlock: ModuleValue[DataRXIO] = InModuleBody { makeCustomIO() }
}

class DataRX(val params: DataRXParams) extends LazyModule()(Parameters.empty) {

  val reorder_data  = Seq.fill(params.channels) {LazyModule(new BitReordering with BitReorderingPins)}
  val reorder_valid = LazyModule(new BitReordering with BitReorderingPins)
  val reorder_frame = LazyModule(new BitReordering with BitReorderingPins)
  val bitslip       = LazyModule(new BitSlipDetection with BitSlipDetectionPins)
  val word_detector = LazyModule(new DetectWordWidth(DetectWordWidthParams(params.channels)) with DetectWordWidthPins)
  val byte2word     = LazyModule(new Byte2Word(Byte2WordParams(params.channels)) with Byte2WordPins)
  val asyncQueue    = if (params.asyncParams.isDefined ) Some(LazyModule(new AXI4StreamAsyncQueueWithControlBlock(params.asyncParams.get) with AXI4StreamAsyncQueueWithControlStandalone{
    override def beatBytes: Int = params.channels*2
  })) else None

  lazy val io: DataRXIO = Wire(new DataRXIO(params.channels, params.asyncParams.isDefined))

  lazy val module: LazyRawModuleImp = new LazyRawModuleImp(this) {
    childClock := io.i_clock
    childReset := io.i_reset

    // bitslip
    bitslip.ioBlock.i_data := io.i_valid
    
    for(i <- 0 until params.channels) {
      // input data
      reorder_data(i).ioBlock.i_data    := withClockAndReset(childClock, childReset) { RegNext(io.i_data(i), 0.U) }
      reorder_data(i).ioBlock.i_bitslip := bitslip.ioBlock.o_bitslip
      // detect word from frame
      word_detector.ioBlock.i_data(i) := reorder_data(i).ioBlock.o_data
      // alligned data to word
      byte2word.ioBlock.i_data(i) := word_detector.ioBlock.o_data(i)
    }
    // input data
    reorder_valid.ioBlock.i_data  := withClockAndReset(childClock, childReset) { RegNext(io.i_valid, 0.U) }
    reorder_frame.ioBlock.i_data  := withClockAndReset(childClock, childReset) { RegNext(io.i_frame, 0.U) }
    //bitslip
    reorder_valid.ioBlock.i_bitslip  := bitslip.ioBlock.o_bitslip
    reorder_frame.ioBlock.i_bitslip  := bitslip.ioBlock.o_bitslip
    // detect word from frame
    word_detector.ioBlock.i_frame := reorder_frame.ioBlock.o_data
    word_detector.ioBlock.i_en    := withClockAndReset(childClock, childReset) { ShiftRegister(bitslip.ioBlock.o_en, 3, 0.U, true.B) }
    // alligned data to word
    byte2word.ioBlock.i_word_size := word_detector.ioBlock.o_word_size
    byte2word.ioBlock.i_en        := word_detector.ioBlock.o_en
    byte2word.ioBlock.i_crc       := word_detector.ioBlock.o_crc

    if(asyncQueue.isDefined) {
      // AsyncQueue (Slave-Side)
      asyncQueue.get.in.bits := DontCare
      asyncQueue.get.pins.write_clock := childClock
      asyncQueue.get.pins.write_reset := childReset
      asyncQueue.get.in.bits.data  := Cat(byte2word.ioBlock.o_data)
      asyncQueue.get.in.valid := byte2word.ioBlock.o_en
      asyncQueue.get.pins.in_ctrl := Cat(byte2word.ioBlock.o_crc, word_detector.ioBlock.o_word_size)
      // AsyncQueue (Master-Side)
      asyncQueue.get.module.clock := io.i_dsp_clock.get
      asyncQueue.get.module.reset := io.i_dsp_reset.get
      io.o_crc := asyncQueue.get.pins.out_ctrl(byte2word.ioBlock.o_crc.getWidth + word_detector.ioBlock.o_word_size.getWidth - 1)
      io.o_word_size := asyncQueue.get.pins.out_ctrl(word_detector.ioBlock.o_word_size.getWidth - 1, 0)
      io.out.bits  := asyncQueue.get.out.bits.data
      io.out.valid := asyncQueue.get.out.valid
      asyncQueue.get.out.ready := io.out.ready
    }
    else {
      io.o_crc := byte2word.ioBlock.o_crc
      io.o_word_size := word_detector.ioBlock.o_word_size
      io.out.bits    := Cat(byte2word.ioBlock.o_data)
      io.out.valid   := byte2word.ioBlock.o_en
    }
  }
}

object DataRXwithAsyncApp extends App
{
    implicit val p: Parameters = Parameters.empty

    val params = DataRXParams(
      channels = 4,
      asyncParams = Some(AXI4StreamAsyncQueueWithControlParams(
        ctrlBits   = 3,
        sync       = 4,
        depth      = 2048,
        safe       = true
      ))
    )
    
    val lazyDut = LazyModule(new DataRX(params) with DataRXPins)
    (new ChiselStage).execute(Array("--target-dir", "verilog/DataRXAsync"), Seq(ChiselGeneratorAnnotation(() => lazyDut.module)))
}

object DataRXwithoutAsyncApp extends App
{
    implicit val p: Parameters = Parameters.empty
    
    val params = DataRXParams(
      channels = 4,
      asyncParams = None
    )
    
    val lazyDut = LazyModule(new DataRX(params) with DataRXPins)
    (new ChiselStage).execute(Array("--target-dir", "verilog/DataRX"), Seq(ChiselGeneratorAnnotation(() => lazyDut.module)))
}