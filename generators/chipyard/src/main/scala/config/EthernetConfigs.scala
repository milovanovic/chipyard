package chipyard

import org.chipsalliance.cde.config.{Config}

// ----------------
// Ethernet Configs
// ----------------

// Verilog RGMII Ethernet loopback with MAC TX connected to MAC RX in the testbench.
// Runs the Ethernet simulation loopback bare-metal program through the MMIO TX/RX queues.
// Run from chipyard root:
//   make -C sims/verilator CONFIG=EthernetRGMIILoopbackRocketConfig run-binary-fast \
//     BINARY=$PWD/software/ethernet/ethernet_sim_loopback.riscv
class EthernetRGMIILoopbackRocketConfig extends Config(
  new chipyard.harness.WithEthernetRGMIILoopback ++           // sim clocks + TX->RX loopback
  new rivet.wrapper.WithEthernetRGMIISim ++                   // MAC built with target="SIM"
  new freechips.rocketchip.rocket.WithCFlushEnabled ++
  new freechips.rocketchip.rocket.WithNHugeCores(1) ++
  new chipyard.config.AbstractConfig)

// Verilog GMII Ethernet loopback with MAC TX connected to MAC RX in the testbench.
// Run from chipyard root:
//   make -C sims/verilator CONFIG=EthernetGMIILoopbackRocketConfig run-binary-fast \
//     BINARY=$PWD/software/ethernet/ethernet_sim_loopback.riscv
class EthernetGMIILoopbackRocketConfig extends Config(
  new chipyard.harness.WithEthernetGMIILoopback ++           // sim clock + GMII TX->RX loopback
  new rivet.wrapper.WithEthernetGMIISim ++                   // MAC built with target="SIM"
  new freechips.rocketchip.rocket.WithCFlushEnabled ++
  new freechips.rocketchip.rocket.WithNHugeCores(1) ++
  new chipyard.config.AbstractConfig)

// Verilog XGMII Ethernet loopback with MAC TX connected to MAC RX in the testbench.
// Run from chipyard root:
//   make -C sims/verilator CONFIG=EthernetXGMIILoopbackRocketConfig run-binary-fast \
//     BINARY=$PWD/software/ethernet/ethernet_sim_loopback.riscv
class EthernetXGMIILoopbackRocketConfig extends Config(
  new chipyard.harness.WithEthernetXGMIILoopback ++            // sim clock + XGMII TX->RX loopback
  new rivet.wrapper.WithEthernetXGMIISim ++                    // MAC instantiated for sim (byte-wide)
  new freechips.rocketchip.rocket.WithCFlushEnabled ++
  new freechips.rocketchip.rocket.WithNHugeCores(1) ++
  new chipyard.config.AbstractConfig)

// Chisel-native RGMII Ethernet loopback using the same MMIO queues and testbench loopback.
// Run from chipyard root:
//   make -C sims/verilator CONFIG=EthMac1GRgmiiLoopbackRocketConfig run-binary-fast \
//     BINARY=$PWD/software/ethernet/ethernet_sim_loopback.riscv
class EthMac1GRgmiiLoopbackRocketConfig extends Config(
  new chipyard.harness.WithEthernetRGMIILoopback ++          // sim clocks + TX->RX loopback (reused)
  new rivet.WithEthernetRGMIISim ++            // Chisel-native MAC, target="SIM"
  new freechips.rocketchip.rocket.WithCFlushEnabled ++
  new freechips.rocketchip.rocket.WithNHugeCores(1) ++
  new chipyard.config.AbstractConfig)

// Chisel-native GMII Ethernet loopback using the same MMIO queues and testbench loopback.
// Run from chipyard root:
//   make -C sims/verilator CONFIG=EthMac1GGmiiLoopbackRocketConfig run-binary-fast \
//     BINARY=$PWD/software/ethernet/ethernet_sim_loopback.riscv
class EthMac1GGmiiLoopbackRocketConfig extends Config(
  new chipyard.harness.WithEthernetGMIILoopback ++           // sim clock + GMII TX->RX loopback (reused)
  new rivet.WithEthernetGMIISim ++             // Chisel-native MAC, target="SIM"
  new freechips.rocketchip.rocket.WithCFlushEnabled ++
  new freechips.rocketchip.rocket.WithNHugeCores(1) ++
  new chipyard.config.AbstractConfig)
