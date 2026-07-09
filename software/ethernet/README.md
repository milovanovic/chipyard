# Ethernet File Streaming

Raw L2 Ethernet file transfer between a Linux PC and the FPGA Ethernet peripheral.

The link uses EtherType `0x88B5` directly, with no IP/ARP stack. The PC sends the actual
bytes in `tx_file.txt`; the FPGA stores them in a 64 KiB static buffer and echoes exactly
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
- Simulation loopback: `software/ethernet/ethernet_sim_loopback.riscv`
- PHY loopback: `software/ethernet/ethernet_phy_loopback.riscv`
- PC: `software/ethernet/pc/ethernet_pc`

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
uart_tsi +tty=/dev/ttyUSB2 ./ethernet_fpga.riscv
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
