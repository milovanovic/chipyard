package chain

import chisel3.{Bool, IO, Output, Vec}
import chisel3.stage.ChiselStage
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts.{IntSinkParameters, IntSinkPortParameters, IntToBundleBridge}
import freechips.rocketchip.tilelink.{BundleBridgeToTL, TLBundle, TLBundleParameters, TLMasterParameters, TLMasterPortParameters}

object DMAEthernetChainDriver extends App {

  // Generate verilog
  (new ChiselStage).emitVerilog(
    args = Array(
      "-X", "verilog",
      "-e", "verilog",
      "--target-dir", "verilog/DMAEtherner"),
    gen = LazyModule(new DMAEthernet(DMAParams.readParams, DMAParams.ethParams){
      val ioCtrlNode = BundleBridgeSource(() =>
        TLBundle(
          TLBundleParameters(
            addressBits = 32,
            dataBits = 32,
            sourceBits = 1,
            sinkBits = 1,
            sizeBits = 6,
            echoFields = Seq(),
            requestFields = Seq(),
            responseFields = Seq(),
            hasBCE = false
          )
        )
      )
      mem.get := BundleBridgeToTL(TLMasterPortParameters.v1(Seq(TLMasterParameters.v1("bundleBridgeToTL")))) := ioCtrlNode
      val io_control = InModuleBody { ioCtrlNode.makeIO() }

      reader.io_read match {
        case axi: AXI4MasterNode => {
          val ioInNode = BundleBridgeSink[AXI4Bundle]()
          ioInNode := AXI4ToBundleBridge(AXI4SlavePortParameters(
            Seq(AXI4SlaveParameters(
              address = Seq(DMAParams.r_readAddress),
              regionType = RegionType.UNCACHED,
              executable = true,
              supportsWrite = TransferSizes(1, DMAParams.readConfig.readDataWidth / 8),
              supportsRead = TransferSizes(1, DMAParams.readConfig.readDataWidth / 8)
            )),
            DMAParams.readConfig.readDataWidth / 8)) := axi
          val io_read = InModuleBody {
            ioInNode.makeIO()
          }
        }
      }

      val r_ioIntNode: BundleBridgeSink[Vec[Bool]] = BundleBridgeSink[Vec[Bool]]()
      r_ioIntNode := IntToBundleBridge(IntSinkPortParameters(Seq(IntSinkParameters(), IntSinkParameters()))) := reader.io_interrupt
      val io_r_int: ModuleValue[Vec[Bool]] = InModuleBody {
        val io = IO(Output(r_ioIntNode.bundle.cloneType))
        io.suggestName("int_r")
        io := r_ioIntNode.bundle
        io
      }
    }).module
  )
}