# Ethernet File Streaming

Raw L2 Ethernet file transfer between a Linux PC and the FPGA Ethernet peripheral.

The link uses EtherType `0x88B5` directly, with no IP/ARP stack.
The PC sends the actual bytes in `tx_file.txt`; the FPGA stores them in a 64 KiB static buffer and echoes exactly
the received length back. The PC verifies CRC32 before writing `rx_file.txt`.

## Build

```bash
cd <chipyard-root>
source env.sh
make -C software/ethernet
make -C software/ethernet/pc
```

Outputs:

- FPGA: `software/ethernet/ethernet_fpga.riscv`
- Simulation file transfer: `software/ethernet/ethernet_sim_file_transfer.riscv`
- Simulation loopback: `software/ethernet/ethernet_sim_loopback.riscv`
- PHY loopback: `software/ethernet/ethernet_phy_loopback.riscv`
- PC: `software/ethernet/pc/ethernet_pc`

## DMA driver and register map

Frames are copied through two buffers in the 64 KiB MBUS scratchpad, then moved between memory and the MAC by the Ethernet DMA:

- TX bounce buffer: `0x0800C000`, 4 KiB
- RX bounce buffer: `0x0800D000`, 4 KiB
- maximum transmitted or received frame: 4096 bytes

The complete map is:

| Base | Block | Access |
|---|---|---|
| `0x10060000` | Ethernet DMA CSR | 64-bit, stride 8 |
| `0x10061000` | frame-length frontend | 64-bit, stride 8 |
| `0x10062000` | MAC status/control | 32-bit |
| `0x10063000` | MDIO | 32-bit |

The DMA register offsets are `ENABLE 0x00`, `WATCHDOG 0x10`, `INT 0x18`, S2M descriptor `0x20..0x40`, M2S descriptor `0x48..0x68`, and `S2M_RESULT 0x90`. `S2M_RESULT[8:0]` is the actual completed beat count and bit 9 indicates that the chunk ended on stream `last`. Frontend offsets are `RX_LEN 0x00` (bit 16 valid, pop-on-read), `RX_COUNT 0x08`, `TX_LEN 0x10`, `TX_SPACE 0x18`, `INFO 0x20`, and `RX_BEATS 0x28`. `RX_BEATS` is a non-destructive count of packed 64-bit beats currently buffered for RX DMA diagnostics.

The MAC status register is sticky read-to-clear. Read it once per checkpoint and cache the returned value; repeated reads clear the TX/RX event evidence.

`eth_send_frame()` copies and fences the TX buffer, queues the exact byte length, then arms one or more M2S chunks of at most 256 beats. `eth_recv_frame()` arms a maximum 256-beat S2M chunk before the frame length is known; stream `last` shortens the final chunk, after which software validates the published RX length. Each chunk waits for its direction's DONE bit and a zero trigger. Completion deliberately does not use `DMA_IDLE`, because a later RX frame may already be buffered. DMA `INT` is shared by both directions and is harvested into a software shadow before it is cleared at a quiescent point.

## Run

Disable NIC offloads before testing:

```bash
sudo ip link set eno1 up
sudo ethtool -K eno1 rx off tx off gro off gso off tso off
```

Run the FPGA program over UART-TSI:

```bash
cd <chipyard-root>
cd software/ethernet
uart_tsi +tty=/dev/ttyUSBX ./ethernet_fpga.riscv
```

Run the PC transfer:

```bash
cd <chipyard-root>
cd software/ethernet/pc
sudo ./ethernet_pc eno1
```

Optional paths:

```bash
sudo ./ethernet_pc eno1 custom_tx.txt custom_rx.txt
```

Verify:

```bash
cmp tx_file.txt rx_file.txt
```

## Verilator file transfer

The file-transfer configurations connect a simulated Ethernet host to the DUT's physical
RGMII, GMII, or XGMII interface. The host reads and verifies binary bytes even when the
files use a `.txt` extension.

Available configurations:

- `EthernetRGMIIFileTransferRocketConfig`: Verilog RGMII MAC
- `EthernetGMIIFileTransferRocketConfig`: Verilog GMII MAC
- `EthernetXGMIIFileTransferRocketConfig`: Verilog XGMII MAC
- `EthMac1GRgmiiFileTransferRocketConfig`: Chisel RGMII MAC
- `EthMac1GGmiiFileTransferRocketConfig`: Chisel GMII MAC

Build and run one configuration from the Chipyard root:

```bash
source env.sh
make -C software/ethernet ethernet_sim_file_transfer.riscv
make -C sims/verilator \
  CONFIG=EthernetRGMIIFileTransferRocketConfig \
  BINARY="$PWD/software/ethernet/ethernet_sim_file_transfer.riscv" \
  LOADMEM=1 \
  EXTRA_SIM_FLAGS="+ethernet-tx-file=$PWD/software/ethernet/pc/tx_file.txt \
    +ethernet-rx-file=$PWD/software/ethernet/pc/rx_file.txt" \
  run-binary-fast
cmp software/ethernet/pc/tx_file.txt software/ethernet/pc/rx_file.txt
```

Both file arguments are required and should use absolute paths:

- `+ethernet-tx-file=/absolute/path/to/input`
- `+ethernet-rx-file=/absolute/path/to/output`

The simulation host retries `START` while firmware boots. Once the first acknowledgement
arrives, protocol retries and progress timeouts are bounded. It publishes the output file
only after validating the complete echo and its CRC32. A missing argument, file error,
malformed response, protocol error, or checksum mismatch terminates the simulation.

## Protocol

- Destination FPGA MAC: `02:00:00:00:00:01`
- EtherType: `0x88B5`
- Maximum file size: `64 KiB`
- Maximum data chunk: `1400` bytes
- Payload header: 28 bytes, manually encoded big-endian fields

Frame types:

- `START`: transfer id, file size, CRC32
- `DATA`: sequence number, offset, chunk length, chunk CRC32, chunk bytes
- `END`: final size and CRC32
- `ACK`: acknowledges accepted `START`, `DATA`, and `END`
- `ERROR`: reports malformed, oversized, sequence, checksum, or timeout failures
