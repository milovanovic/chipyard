# Queue example (SInt, UInt, FixedPoint)

In the newer versions of the chipyard (above tag 1.8.1), when using the Chipyard's makeflow to generate verilog for Queue with input data type SInt (or FixedPoint), the following problem occurs:
```scala
Decoupled.scala:273:95: error: memories should be flattened before running LowerMemory
Decoupled.scala:273:95: note: see current operation: %4:2 = "firrtl.mem"() {annotations = [], depth = 2 : i64, name = "ram", nameKind = #firrtl<name_kind droppable_name>, portAnnotations = [[], []], portNames = ["MPORT", "io_deq_bits_MPORT"], readLatency = 0 : i32, ruw = 0 : i32, writeLatency = 1 : i32} : () -> (!firrtl.bundle<addr: uint<1>, en: uint<1>, clk: clock, data: sint<16>, mask: uint<1>>, !firrtl.bundle<addr: uint<1>, en: uint<1>, clk: clock, data flip: sint<16>>)
```

The Queue code example is just simple Queue "connected" to the bus, the code can be found [here](./generators/chipyard/src/main/scala/Queue/QueueExample.scala).

This issue occurred between tags 1.8.1 and 1.9.0 because this flow works on tag 1.8.1 but fails on tag 1.9.0. It should be noted that everything works when UInt data is supplied to Queue. The main issue is that the flow for Queue SInt fails. FixedPoint from the [fixedpoint](https://github.com/ucb-bar/fixedpoint) library likely fails because it employs SInt.

To reporoduce this error, please see branch [1.9.0_queue](https://github.com/milovanovic/chipyard/tree/1.9.0_queue).

To generate verilog here, run:
```bash
cd sims/verilator
make verilog CONFIG=SIntQueueConfig # for SInt Queue
make verilog CONFIG=FixedPointQueueConfig # for FixedPoint Queue
make verilog CONFIG=UIntQueueConfig # for FixedPoint Queue
```
