// Baremetal Ethernet streaming demo for RocketNexysVideoConfig (Nexys Video board).
//
// 1. Brings up the RGMII MAC.
// 2. Transmits a burst of frames to the PC, each carrying an incrementing 32-bit counter.
// 3. Then loops forever: receives frames from the PC, prints a summary over the UART/HTIF
//    console, and echoes each frame back to its sender.
//
// Pair this with software/eth-stream/pc/raw_eth.c on the PC:
//   sudo ./raw_eth rx <ifname>          # observe the TX burst from the SoC
//   sudo ./raw_eth tx <ifname> [count]  # send frames that the SoC will echo
//
// printf is delivered to the host via HTIF (the uart_tsi host program on the PC).

#include "eth.h"
#include "rtl8211e.h"
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

enum {
  kEthertype  = 0x88B5, // local experimental EtherType
  kTxBurst    = 16,     // frames to send on startup
  kPayloadLen = 46,     // -> 60 B frame, MAC pads/CRCs to 64 B
};

static const uint8_t kSocMac[6]   = {0x02, 0x00, 0x00, 0x00, 0x00, 0x01};
static const uint8_t kBcastMac[6] = {0xff, 0xff, 0xff, 0xff, 0xff, 0xff};

static uint8_t txbuf[2048];
static uint8_t rxbuf[2048];

static const char *speed_str(uint32_t s) {
  switch (s) {
    case 0: return "10M";
    case 1: return "100M";
    case 2: return "1G";
    default: return "?";
  }
}

int main(void) {
  printf("[eth] init\n");
  // Bring up the PHY over MDIO first: confirm it's alive, enable RGMII delay, kick autoneg.
  rtl8211e_bringup(printf);
  eth_init();
  printf("[eth] status=0x%lx link=%s\n", (unsigned long)eth_status(),
         speed_str(eth_link_speed()));

  // TX burst: send kTxBurst counter frames to broadcast, which the PC can receive and verify.
  printf("[eth] sending %d frames\n", kTxBurst);
  for (uint32_t seq = 0; seq < kTxBurst; seq++) {
    uint8_t payload[kPayloadLen];
    memset(payload, 0, sizeof(payload));
    payload[0] = (uint8_t)(seq >> 24);
    payload[1] = (uint8_t)(seq >> 16);
    payload[2] = (uint8_t)(seq >> 8);
    payload[3] = (uint8_t)(seq);
    for (int i = 4; i < kPayloadLen; i++) {
      payload[i] = (uint8_t)i;
    }

    int len = eth_build_frame(txbuf, kBcastMac, kSocMac, kEthertype, payload, kPayloadLen);
    eth_send_frame(txbuf, len);
  }
  printf("[eth] tx done, status=0x%lx\n", (unsigned long)eth_status());

  // RX/echo loop: print a summary of each received frame and echo it back to its sender (swap dst/src).
  printf("[eth] entering rx/echo loop\n");
  uint32_t rxn = 0;
  while (true) {
    int len = eth_recv_frame(rxbuf, sizeof(rxbuf));
    int stored = (len <= (int)sizeof(rxbuf)) ? len : (int)sizeof(rxbuf);
    uint32_t seq = 0;
    if (stored >= 18) {
      seq = ((uint32_t)rxbuf[14] << 24) | ((uint32_t)rxbuf[15] << 16) |
            ((uint32_t)rxbuf[16] << 8) | (uint32_t)rxbuf[17];
    }
    printf("[eth] rx #%lu len=%d type=0x%02x%02x payload_seq=%lu status=0x%lx\n",
           (unsigned long)rxn, len, rxbuf[12], rxbuf[13], (unsigned long)seq,
           (unsigned long)eth_status());

    // Echo back to sender: swap dst/src (src of received becomes dst).
    if (stored >= 12) {
      uint8_t echo[2048];
      memcpy(echo, rxbuf, stored);
      for (int i = 0; i < 6; i++) {
        echo[i] = rxbuf[6 + i];   // dst = original src
        echo[6 + i] = kSocMac[i]; // src = us
      }
      eth_send_frame(echo, stored);
    }
    rxn++;
  }
  return 0;
}
