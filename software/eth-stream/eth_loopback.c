// Self-checking RGMII Ethernet loopback test (for EthernetRGMIILoopbackRocketConfig).
//
// In the RTL simulation harness the MAC's RGMII TX pins are wired straight back to its RX pins.
// This program:
//   1. brings up the MAC,
//   2. transmits one frame with a known payload through TLWriteQueueWithLast,
//   3. waits (bounded) for the looped-back frame on TLReadQueueWithLast,
//   4. verifies header + payload and the frame's `last` delimiter,
//   5. prints LOOPBACK PASS / LOOPBACK FAIL and returns 0 / nonzero (HTIF pass/fail).
//
// It exercises the exact data path that was broken and fixed: software MMIO -> frame-aware
// TX queue -> MAC TX framing (tlast) -> MAC RX framing -> frame-aware RX queue -> software.
//
// printf is delivered to the host over HTIF.

#include "eth.h"
#include <stdint.h>
#include <stdio.h>
#include <string.h>

enum {
  kEthertype  = 0x88B5,
  kPayloadLen = 46, // 14 + 46 = 60 B frame; MAC appends 4 B FCS -> 64 B on wire
  // Bound on how long to spin waiting for the looped-back frame before declaring failure,
  // so a broken data path fails cleanly instead of hanging the simulation.
  kRxWaitSpins = 2000000,
};

// In loopback there is no MAC-address filtering, so these are arbitrary but checkable.
static const uint8_t kDstMac[6] = {0x02, 0x00, 0x00, 0x00, 0x00, 0x02};
static const uint8_t kSrcMac[6] = {0x02, 0x00, 0x00, 0x00, 0x00, 0x01};

static uint8_t txbuf[2048];
static uint8_t rxbuf[2048];

int main(void) {
  printf("[loopback] init\n");
  eth_init();
  printf("[loopback] status=0x%lx\n", (unsigned long)eth_status());

  // Build a frame with a known, position-dependent payload pattern.
  uint8_t payload[kPayloadLen];
  for (int i = 0; i < kPayloadLen; i++) {
    payload[i] = (uint8_t)(0xA0 + i);
  }
  int txlen = eth_build_frame(txbuf, kDstMac, kSrcMac, kEthertype, payload, kPayloadLen);

  printf("[loopback] tx frame len=%d\n", txlen);
  eth_send_frame(txbuf, txlen);

  // Bounded wait for the first received byte.
  printf("[loopback] waiting for looped-back frame...\n");
  long spin = 0;
  while (eth_r32(ETH_RX_COUNT) == 0) {
    if (++spin > kRxWaitSpins) {
      printf("[loopback] timeout: no rx, status=0x%lx\n", (unsigned long)eth_status());
      printf("LOOPBACK FAIL\n");
      return 1;
    }
  }

  int rxlen = eth_recv_frame(rxbuf, sizeof(rxbuf));
  uint32_t st = eth_status();
  printf("[loopback] rx len=%d status=0x%lx\n", rxlen, (unsigned long)st);

  // Verify header + payload prefix.
  // Tolerant to FCS being stripped/kept and to padding.
  // The leading dst/src/ethertype/payload bytes are deterministic regardless.
  int ok = 1;
  if (rxlen < txlen) {
    printf("[loopback] FAIL: rx len %d < tx len %d\n", rxlen, txlen);
    ok = 0;
  }
  for (int i = 0; ok && i < 6; i++) {
    if (rxbuf[i] != kDstMac[i]) {
      printf("[loopback] FAIL dst[%d]=0x%02x\n", i, rxbuf[i]);
      ok = 0;
    }
  }
  for (int i = 0; ok && i < 6; i++) {
    if (rxbuf[6 + i] != kSrcMac[i]) {
      printf("[loopback] FAIL src[%d]=0x%02x\n", i, rxbuf[6 + i]);
      ok = 0;
    }
  }
  if (ok && (rxbuf[12] != (uint8_t)(kEthertype >> 8) || rxbuf[13] != (uint8_t)(kEthertype & 0xFF))) {
    printf("[loopback] FAIL ethertype 0x%02x%02x\n", rxbuf[12], rxbuf[13]);
    ok = 0;
  }
  for (int i = 0; ok && i < kPayloadLen; i++) {
    if (rxbuf[14 + i] != payload[i]) {
      printf("[loopback] FAIL payload[%d]=0x%02x exp 0x%02x\n", i, rxbuf[14 + i], payload[i]);
      ok = 0;
    }
  }

  if (st & (ETH_ST_RX_ERR_BAD_FRAME | ETH_ST_RX_ERR_BAD_FCS | ETH_ST_RX_FIFO_OVERFLOW)) {
    printf("[loopback] WARN rx error/overflow bits set: 0x%lx\n", (unsigned long)st);
  }

  if (ok) {
    printf("LOOPBACK PASS\n");
    return 0;
  }
  printf("LOOPBACK FAIL\n");
  return 1;
}
