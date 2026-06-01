// Baremetal driver for the rivet RGMII Ethernet peripheral on RocketNexysVideoConfig.
//
// Memory map (PBUS absolute addresses, see rivet.wrapper.WithEthernetRGMII):
//   TX write queue  base 0x2000 : +0x0 push byte (last=0)
//                                 +0x4 entries used (RO)
//                                 +0x8 push byte (last=1, closes & sends the frame)
//   RX read  queue  base 0x2400 : +0x0 pop {bit8=last, [7:0]=data} (RO, advances queue)
//                                 +0x4 entries used (RO)
//   MAC CSR         base 0x2800 : +0x0 status (RO)
//                                 +0x4 control (WO)
//
// The queues are byte-wide and carry the AXI4-Stream `last` flag.
// So a frame is the run of bytes written to 0x2000 terminated by a single byte written to 0x2008.
// The MAC inserts the FCS (CRC) and pads short frames to 64 B, so software must NOT append a CRC.

#ifndef RIVET_ETH_H
#define RIVET_ETH_H

#include <stdbool.h>
#include <stdint.h>

/**
 * Write a 32-bit word to MMIO address `a`.
 *
 * @param a MMIO address.
 * @param v Value to write.
 */
inline void eth_w32(uintptr_t a, uint32_t v) { *(volatile uint32_t *)a = v; }

/**
 * Read a 32-bit word from MMIO address `a`.
 *
 * @param a MMIO address.
 * @return The value read.
 */
inline uint32_t eth_r32(uintptr_t a) { return *(volatile uint32_t *)a; }

// Register addresses
#define ETH_TX_BASE   0x2000UL
#define ETH_TX_DATA   (ETH_TX_BASE + 0x0) // write byte, last=0
#define ETH_TX_COUNT  (ETH_TX_BASE + 0x4) // entries used
#define ETH_TX_LAST   (ETH_TX_BASE + 0x8) // write byte, last=1

#define ETH_RX_BASE   0x2400UL
#define ETH_RX_DATA   (ETH_RX_BASE + 0x0) // read {last<<8 | data}
#define ETH_RX_COUNT  (ETH_RX_BASE + 0x4) // entries used

#define ETH_CSR_BASE  0x2800UL
#define ETH_STATUS    (ETH_CSR_BASE + 0x0)
#define ETH_CONTROL   (ETH_CSR_BASE + 0x4)

#define ETH_TX_QUEUE_DEPTH 64
#define ETH_RX_QUEUE_DEPTH 64

// Control register fields
#define ETH_CTRL_IFG(x) ((uint32_t)((x) & 0xFF)) // inter-frame gap (default 12)
#define ETH_CTRL_TX_EN  (1u << 8)
#define ETH_CTRL_RX_EN  (1u << 9)

// Status register fields
#define ETH_ST_TX_ERR_UNDERFLOW (1u << 0)
#define ETH_ST_TX_FIFO_OVERFLOW (1u << 1)
#define ETH_ST_TX_FIFO_BAD_FR   (1u << 2)
#define ETH_ST_TX_FIFO_GOOD_FR  (1u << 3)
#define ETH_ST_RX_ERR_BAD_FRAME (1u << 4)
#define ETH_ST_RX_ERR_BAD_FCS   (1u << 5)
#define ETH_ST_RX_FIFO_OVERFLOW (1u << 6)
#define ETH_ST_RX_FIFO_BAD_FR   (1u << 7)
#define ETH_ST_RX_FIFO_GOOD_FR  (1u << 8)
#define ETH_ST_SPEED_SHIFT      9
#define ETH_ST_SPEED_MASK       (0x3u << ETH_ST_SPEED_SHIFT) // 0=10M 1=100M 2=1G

/**
 * Enable TX and RX with the default inter-frame gap.
 */
inline void eth_init(void) {
  eth_w32(ETH_CONTROL, ETH_CTRL_IFG(12) | ETH_CTRL_TX_EN | ETH_CTRL_RX_EN);
}

/**
 * Read the MAC status register.
 *
 * @return The raw status word (`ETH_ST_*` fields).
 */
inline uint32_t eth_status(void) { return eth_r32(ETH_STATUS); }

/**
 * Report the negotiated link speed.
 *
 * @return 0 = 10M, 1 = 100M, 2 = 1G.
 */
inline uint32_t eth_link_speed(void) {
  return (eth_status() & ETH_ST_SPEED_MASK) >> ETH_ST_SPEED_SHIFT;
}

/**
 * Transmit one L2 frame (dst[6] + src[6] + ethertype[2] + payload). Do NOT include FCS.
 *
 * Blocks while the small staging queue is full.
 *
 * @param buf Frame bytes.
 * @param len Frame length in bytes (must be >= 1).
 */
inline void eth_send_frame(const uint8_t *buf, int len) {
  for (int i = 0; i < len; i++) {
    while (eth_r32(ETH_TX_COUNT) >= ETH_TX_QUEUE_DEPTH) {
      // wait for space
    }
    if (i == len - 1) {
      eth_w32(ETH_TX_LAST, buf[i]);
    } else {
      eth_w32(ETH_TX_DATA, buf[i]);
    }
  }
}

/**
 * Receive one L2 frame into `buf`. Blocks until a frame arrives.
 *
 * If the frame is longer than `maxlen`, the excess bytes are drained but not stored; the
 * full length is still returned so the caller can detect truncation (return value > maxlen).
 *
 * @param buf    Destination buffer.
 * @param maxlen Capacity of `buf` in bytes.
 * @return The received length in bytes (FCS already stripped by the MAC).
 */
inline int eth_recv_frame(uint8_t *buf, int maxlen) {
  int n = 0;
  while (true) {
    while (eth_r32(ETH_RX_COUNT) == 0) {
      // wait for data
    }
    uint32_t w = eth_r32(ETH_RX_DATA);
    if (n < maxlen) {
      buf[n] = (uint8_t)(w & 0xFF);
    }
    n++;
    if ((w >> 8) & 0x1) {
      break; // last byte of frame
    }
  }
  return n;
}

/**
 * Non-blocking receive.
 *
 * NOTE: only call when you intend to consume a whole frame (it blocks mid-frame).
 *
 * @param buf    Destination buffer.
 * @param maxlen Capacity of `buf` in bytes.
 * @return -1 if no complete frame is ready yet, else the frame length.
 */
inline int eth_try_recv_frame(uint8_t *buf, int maxlen) {
  if (eth_r32(ETH_RX_COUNT) == 0) {
    return -1;
  }
  return eth_recv_frame(buf, maxlen);
}

/**
 * Build an L2 header into `buf` and copy the payload after it.
 *
 * @param buf       Destination buffer.
 * @param dst       Destination MAC (6 bytes).
 * @param src       Source MAC (6 bytes).
 * @param ethertype EtherType value.
 * @param payload   Payload bytes.
 * @param plen      Payload length in bytes.
 * @return The total frame length.
 */
inline int eth_build_frame(uint8_t *buf, const uint8_t dst[6], const uint8_t src[6],
                           uint16_t ethertype, const uint8_t *payload, int plen) {
  int n = 0;
  for (int i = 0; i < 6; i++) {
    buf[n++] = dst[i];
  }
  for (int i = 0; i < 6; i++) {
    buf[n++] = src[i];
  }
  buf[n++] = (uint8_t)(ethertype >> 8);
  buf[n++] = (uint8_t)(ethertype & 0xFF);
  for (int i = 0; i < plen; i++) {
    buf[n++] = payload[i];
  }
  return n;
}

#endif // RIVET_ETH_H
