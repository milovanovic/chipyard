// Bare-metal driver for the RIVET RGMII Ethernet peripheral.

#ifndef SOFTWARE_ETHERNET_ETH_H_
#define SOFTWARE_ETHERNET_ETH_H_

#include "software/ethernet/mmio_config.h"

#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Write a 32-bit word to MMIO address `a`.
 *
 * @param a MMIO address.
 * @param v Value to write.
 */
static inline void eth_w32(uintptr_t a, uint32_t v) { *(volatile uint32_t *)a = v; }

/**
 * Read a 32-bit word from MMIO address `a`.
 *
 * @param a MMIO address.
 * @return The value read.
 */
static inline uint32_t eth_r32(uintptr_t a) { return *(volatile uint32_t *)a; }

#define ETH_TX_BASE ETHERNET_TX_BASE
#define ETH_TX_DATA (ETH_TX_BASE + 0x0)
#define ETH_TX_COUNT (ETH_TX_BASE + 0x4)
#define ETH_TX_LAST (ETH_TX_BASE + 0x8)

#define ETH_RX_BASE ETHERNET_RX_BASE
#define ETH_RX_DATA (ETH_RX_BASE + 0x0)
#define ETH_RX_COUNT (ETH_RX_BASE + 0x4)

#define ETH_CSR_BASE ETHERNET_CSR_BASE
#define ETH_STATUS (ETH_CSR_BASE + 0x0)
#define ETH_CONTROL (ETH_CSR_BASE + 0x4)

#define ETH_TX_QUEUE_DEPTH 64
#define ETH_RX_QUEUE_DEPTH 64

#define ETH_CTRL_IFG(x) ((uint32_t)((x) & 0xffu))
#define ETH_CTRL_TX_EN (1u << 8)
#define ETH_CTRL_RX_EN (1u << 9)

#define ETH_ST_TX_ERR_UNDERFLOW (1u << 0)
#define ETH_ST_TX_FIFO_OVERFLOW (1u << 1)
#define ETH_ST_TX_FIFO_BAD_FR (1u << 2)
#define ETH_ST_TX_FIFO_GOOD_FR (1u << 3)
#define ETH_ST_RX_ERR_BAD_FRAME (1u << 4)
#define ETH_ST_RX_ERR_BAD_FCS (1u << 5)
#define ETH_ST_RX_FIFO_OVERFLOW (1u << 6)
#define ETH_ST_RX_FIFO_BAD_FR (1u << 7)
#define ETH_ST_RX_FIFO_GOOD_FR (1u << 8)
#define ETH_ST_SPEED_SHIFT 9
#define ETH_ST_SPEED_MASK (0x3u << ETH_ST_SPEED_SHIFT)

/**
 * Enable Ethernet TX and RX with the default inter-frame gap.
 */
static inline void eth_init(void) {
  eth_w32(ETH_CONTROL, ETH_CTRL_IFG(12) | ETH_CTRL_TX_EN | ETH_CTRL_RX_EN);
}

/**
 * Read the MAC status register.
 *
 * @return The raw status word (`ETH_ST_*` fields).
 */
static inline uint32_t eth_status(void) { return eth_r32(ETH_STATUS); }

/**
 * Report the negotiated link speed.
 *
 * @return 0 = 10M, 1 = 100M, 2 = 1G.
 */
static inline uint32_t eth_link_speed(void) {
  return (eth_status() & ETH_ST_SPEED_MASK) >> ETH_ST_SPEED_SHIFT;
}

/**
 * Transmit one L2 frame. Do not include FCS.
 *
 * @param buf Frame bytes.
 * @param len Frame length in bytes. Must be at least 1.
 */
static inline void eth_send_frame(const uint8_t *buf, int len) {
  for (int i = 0; i < len; i++) {
    while (eth_r32(ETH_TX_COUNT) >= ETH_TX_QUEUE_DEPTH) {
      // Wait for queue space.
    }
    if (i == len - 1) {
      eth_w32(ETH_TX_LAST, buf[i]);
    } else {
      eth_w32(ETH_TX_DATA, buf[i]);
    }
  }
}

/**
 * Receive one L2 frame into `buf`.
 *
 * If the frame is longer than `maxlen`, the excess bytes are drained and the full received length is still returned.
 *
 * @param buf Destination buffer.
 * @param maxlen Capacity of `buf`.
 * @return Received frame length in bytes.
 */
static inline int eth_recv_frame(uint8_t *buf, int maxlen) {
  int n = 0;
  while (true) {
    while (eth_r32(ETH_RX_COUNT) == 0) {
      // Wait for data.
    }
    uint32_t word = eth_r32(ETH_RX_DATA);
    if (n < maxlen) {
      buf[n] = (uint8_t)(word & 0xffu);
    }
    n++;
    if ((word >> 8) & 0x1u) {
      break;
    }
  }
  return n;
}

#ifdef __cplusplus
}  // extern "C"
#endif

#endif  // SOFTWARE_ETHERNET_ETH_H_
