// Self-checking RGMII Ethernet loopback test for simulation.

#include "software/ethernet/eth.h"

#include <stdint.h>
#include <stdio.h>

enum {
  kEthertype = 0x88B5,
  kPayloadLen = 46,
  kFrameLen = 14 + kPayloadLen,
  kRxWaitSpins = 2000000,
};

static const uint8_t kDstMac[6] = {0x02, 0x00, 0x00, 0x00, 0x00, 0x02};
static const uint8_t kSrcMac[6] = {0x02, 0x00, 0x00, 0x00, 0x00, 0x01};

static uint8_t tx_frame[kFrameLen];
static uint8_t rx_frame[kFrameLen];

// Fill the loopback payload with a deterministic byte pattern.
static void fill_payload(uint8_t payload[kPayloadLen]) {
  for (int i = 0; i < kPayloadLen; i++) {
    payload[i] = (uint8_t)(0xa0 + i);
  }
}

// Build the fixed Ethernet frame used by the simulation loopback test.
static int build_test_frame(uint8_t frame[kFrameLen], const uint8_t payload[kPayloadLen]) {
  int n = 0;
  for (int i = 0; i < 6; i++) {
    frame[n++] = kDstMac[i];
  }
  for (int i = 0; i < 6; i++) {
    frame[n++] = kSrcMac[i];
  }
  frame[n++] = (uint8_t)(kEthertype >> 8);
  frame[n++] = (uint8_t)(kEthertype & 0xffu);
  for (int i = 0; i < kPayloadLen; i++) {
    frame[n++] = payload[i];
  }
  return n;
}

// Wait until the simulated MAC has a received frame ready.
static int wait_for_rx_ready(void) {
  for (long spin = 0; eth_r32(ETH_RX_COUNT) == 0; spin++) {
    if (spin >= kRxWaitSpins) {
      printf("[simloop] timeout: no rx, status=0x%lx\n", (unsigned long)eth_status());
      return -1;
    }
  }
  return 0;
}

// Verify a byte range in the received frame.
static int verify_bytes(const char *name, int offset, const uint8_t *expected, int len) {
  for (int i = 0; i < len; i++) {
    if (rx_frame[offset + i] != expected[i]) {
      printf("[simloop] FAIL %s[%d]=0x%02x exp 0x%02x\n", name, i,
             rx_frame[offset + i], expected[i]);
      return -1;
    }
  }
  return 0;
}

// Verify the received frame contents and report RX status warnings.
static int verify_frame(int rx_len, int tx_len, const uint8_t payload[kPayloadLen],
                        uint32_t status) {
  if (rx_len < tx_len) {
    printf("[simloop] FAIL: rx len %d < tx len %d\n", rx_len, tx_len);
    return -1;
  }
  if (verify_bytes("dst", 0, kDstMac, 6) < 0 ||
      verify_bytes("src", 6, kSrcMac, 6) < 0 ||
      verify_bytes("payload", 14, payload, kPayloadLen) < 0) {
    return -1;
  }
  if (rx_frame[12] != (uint8_t)(kEthertype >> 8) ||
      rx_frame[13] != (uint8_t)(kEthertype & 0xffu)) {
    printf("[simloop] FAIL ethertype 0x%02x%02x\n", rx_frame[12], rx_frame[13]);
    return -1;
  }

  uint32_t error_bits = ETH_ST_RX_ERR_BAD_FRAME | ETH_ST_RX_ERR_BAD_FCS |
                        ETH_ST_RX_FIFO_OVERFLOW;
  if ((status & error_bits) != 0) {
    printf("[simloop] WARN rx error/overflow bits set: 0x%lx\n",
           (unsigned long)status);
  }
  return 0;
}

// Run one simulated Ethernet loopback send/receive/verify cycle.
int main(void) {
  printf("[simloop] init\n");
  eth_init();
  printf("[simloop] status=0x%lx\n", (unsigned long)eth_status());

  uint8_t payload[kPayloadLen];
  fill_payload(payload);
  int tx_len = build_test_frame(tx_frame, payload);

  printf("[simloop] tx frame len=%d\n", tx_len);
  eth_send_frame(tx_frame, tx_len);

  printf("[simloop] waiting for looped-back frame...\n");
  if (wait_for_rx_ready() < 0) {
    printf("LOOPBACK FAIL\n");
    return 1;
  }

  int rx_len = eth_recv_frame(rx_frame, (int)sizeof(rx_frame));
  uint32_t status = eth_status();
  printf("[simloop] rx len=%d status=0x%lx\n", rx_len, (unsigned long)status);

  if (verify_frame(rx_len, tx_len, payload, status) < 0) {
    printf("LOOPBACK FAIL\n");
    return 1;
  }

  printf("LOOPBACK PASS\n");
  return 0;
}
