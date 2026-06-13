// Baremetal Ethernet streaming demo for RocketNexysVideoConfig (Nexys Video board).
//
// 1. Brings up the RGMII MAC and waits for link.
// 2. Transmits a burst of frames to the PC, each carrying an incrementing 32-bit counter.
// 3. Then loops forever: receives frames from the PC. It ignores background LAN traffic and
//    only acts on our own EtherType (0x88B5), checking each frame's sequence counter against
//    the expected next value, printing a clear OK/SEQ-GAP verdict, and echoing it back.
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
  kEthertype   = 0x88B5,  // local experimental EtherType
  kTxBurst     = 16,      // frames to send on startup
  kPayloadLen  = 46,      // -> 60 B frame, MAC pads/CRCs to 64 B
  kLinkPollMax = 1000000, // link-up poll cap (autoneg takes ~2-3 s)
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

// EtherType is the big-endian 16-bit field at offset 12 of an L2 frame.
static uint16_t frame_ethertype(const uint8_t *f) {
  return (uint16_t)(((uint16_t)f[12] << 8) | f[13]);
}

// Our payload carries a 32-bit big-endian sequence counter in its first 4 bytes (offset 14).
static uint32_t frame_seq(const uint8_t *f) {
  return ((uint32_t)f[14] << 24) | ((uint32_t)f[15] << 16) |
         ((uint32_t)f[16] << 8) | (uint32_t)f[17];
}

int main(void) {
  setvbuf(stdout, NULL, _IONBF, 0);
  printf("[eth] init\n");
  // Bring up the PHY over MDIO first: confirm it's alive, enable RGMII delay, kick autoneg.
  int phy = rtl8211e_bringup(printf);
  eth_init();

  // Autoneg takes ~2-3 s. Poll for link before transmitting so the startup burst doesn't
  // hit a still-down link, and so the speed we print is the negotiated one.
  printf("[eth] waiting for link...\n");
  bool link = false;
  for (uint32_t spins = 0; spins < kLinkPollMax; spins++) {
    if (phy >= 0 && rtl8211e_link_up(phy)) {
      link = true;
      break;
    }
  }
  if (link) {
    printf("[eth] LINK UP  speed=%s status=0x%lx\n", speed_str(eth_link_speed()),
           (unsigned long)eth_status());
  } else {
    printf("[eth] LINK DOWN (timed out) - check cable/PHY; continuing anyway\n");
  }

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

  // RX/echo loop. The MAC has no address filter, so we receive all LAN traffic; ignore
  // everything except our own EtherType. For our frames, check the sequence counter against
  // the expected next value and echo the frame back to its sender (swap dst/src).
  printf("[eth] entering rx/echo loop, watching EtherType 0x%04x (ignoring other traffic)\n",
         kEthertype);
  uint32_t ours = 0;     // frames received with our EtherType
  uint32_t ignored = 0;  // background frames skipped
  uint32_t expected = 0; // next sequence counter we expect from the PC
  while (true) {
    int len = eth_recv_frame(rxbuf, sizeof(rxbuf));
    int stored = (len <= (int)sizeof(rxbuf)) ? len : (int)sizeof(rxbuf);

    if (stored < 14 || frame_ethertype(rxbuf) != kEthertype) {
      ignored++;
      continue; // background LAN noise - don't log or echo it
    }

    uint32_t seq = (stored >= 18) ? frame_seq(rxbuf) : 0;
    const char *verdict = (seq == expected) ? "OK" : "SEQ-GAP";
    printf("[eth] rx ours #%lu  seq=%lu (expected %lu) %s  len=%d  status=0x%lx  [%lu bg frames ignored]\n",
           (unsigned long)ours, (unsigned long)seq, (unsigned long)expected, verdict, len,
           (unsigned long)eth_status(), (unsigned long)ignored);
    expected = seq + 1;
    ours++;

    // Echo back to sender: swap dst/src (src of received becomes dst).
    uint8_t echo[2048];
    memcpy(echo, rxbuf, stored);
    for (int i = 0; i < 6; i++) {
      echo[i] = rxbuf[6 + i];   // dst = original src
      echo[6 + i] = kSocMac[i]; // src = us
    }
    eth_send_frame(echo, stored);
  }
  return 0;
}
