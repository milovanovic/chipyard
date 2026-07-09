// On-board RGMII loopback self-test using the RTL8211E internal loopback.

#include "software/ethernet/eth.h"
#include "software/ethernet/rtl8211e.h"

#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>

enum {
  kEthertype = 0x88B5,
  kPayloadLen = 46,
  kFrameLen = 14 + kPayloadLen,
  kMdioWaitSpins = 2000000,
  kTxWaitSpins = 2000000,
  kRxWaitSpins = 50000000,
  kPhySettle = 50000000,
};

static const uint8_t kDstMac[6] = {0x02, 0x00, 0x00, 0x00, 0x00, 0x02};
static const uint8_t kSrcMac[6] = {0x02, 0x00, 0x00, 0x00, 0x00, 0x01};

static uint8_t tx_frame[kFrameLen];
static uint8_t rx_frame[kFrameLen];

// Print a compact snapshot of MAC and MDIO state.
static void print_status_snapshot(const char *where) {
  printf("[phyloop] %s: mdio_stat=0x%lx tx_count=%lu rx_count=%lu eth_status=0x%lx\n",
         where, (unsigned long)mdio_r32(MDIO_STAT), (unsigned long)eth_r32(ETH_TX_COUNT),
         (unsigned long)eth_r32(ETH_RX_COUNT), (unsigned long)eth_status());
}

// Wait for the MDIO master to become idle with a bounded timeout.
static int mdio_wait_idle_bounded(const char *where) {
  for (long spin = 0; spin < kMdioWaitSpins; spin++) {
    uint32_t status = mdio_r32(MDIO_STAT);
    if ((status & MDIO_STAT_BUSY) == 0) {
      return 0;
    }
  }
  printf("[phyloop] timeout: MDIO busy during %s\n", where);
  print_status_snapshot("mdio timeout");
  return -1;
}

// Read one PHY register with bounded MDIO waits.
static int mdio_read_bounded(int phy, int reg, uint16_t *value, const char *where) {
  if (mdio_wait_idle_bounded(where) < 0) {
    return -1;
  }
  mdio_w32(MDIO_CMD, mdio_pack_cmd(phy, reg, MDIO_OP_READ));
  if (mdio_wait_idle_bounded(where) < 0) {
    return -1;
  }
  *value = (uint16_t)(mdio_r32(MDIO_RDATA) & 0xffffu);
  return 0;
}

// Write one PHY register with bounded MDIO waits.
static int mdio_write_bounded(int phy, int reg, uint16_t value, const char *where) {
  if (mdio_wait_idle_bounded(where) < 0) {
    return -1;
  }
  mdio_w32(MDIO_WDATA, value);
  mdio_w32(MDIO_CMD, mdio_pack_cmd(phy, reg, MDIO_OP_WRITE));
  return mdio_wait_idle_bounded(where);
}

// Read, modify, and write one PHY register with bounded MDIO waits.
static int mdio_rmw_bounded(int phy, int reg, uint16_t clear, uint16_t set,
                            const char *where) {
  uint16_t value = 0;
  if (mdio_read_bounded(phy, reg, &value, where) < 0) {
    return -1;
  }
  value = (uint16_t)((value & ~clear) | set);
  return mdio_write_bounded(phy, reg, value, where);
}

// Scan the MDIO bus for an RTL8211E PHY.
static int rtl8211e_find_phy_bounded(uint16_t *id1_out, uint16_t *id2_out) {
  for (int phy = 0; phy < 32; phy++) {
    uint16_t id1 = 0;
    uint16_t id2 = 0;
    if (mdio_read_bounded(phy, MII_PHYID1, &id1, "find PHYID1") < 0 ||
        mdio_read_bounded(phy, MII_PHYID2, &id2, "find PHYID2") < 0) {
      return -2;
    }
    if (id1 == RTL8211E_ID1 && (id2 & RTL8211E_ID2_MASK) == RTL8211E_ID2) {
      *id1_out = id1;
      *id2_out = id2;
      return phy;
    }
  }
  return -1;
}

// Configure the RTL8211E RGMII clock delay bits.
static int rtl8211e_set_rgmii_delay_bounded(int phy, int rx, int tx) {
  uint16_t set = (uint16_t)((rx ? RTL8211E_RX_DELAY : 0) | (tx ? RTL8211E_TX_DELAY : 0));
  if (mdio_write_bounded(phy, RTL_PAGE_SELECT, 0x0007, "select ext page mode") < 0 ||
      mdio_write_bounded(phy, RTL_EXT_PAGE_SELECT, RTL8211E_EXT_PAGE,
                         "select RGMII page") < 0 ||
      mdio_rmw_bounded(phy, RTL8211E_RGMII_CFG_REG,
                       RTL8211E_RX_DELAY | RTL8211E_TX_DELAY, set,
                       "set RGMII delay") < 0 ||
      mdio_write_bounded(phy, RTL_PAGE_SELECT, 0x0000, "restore page 0") < 0) {
    return -1;
  }
  return 0;
}

// Put the PHY into forced gigabit near-end loopback.
static int phy_enable_loopback_bounded(int phy) {
  if (mdio_write_bounded(phy, MII_CTRL1000, CTRL1000_MANUAL | CTRL1000_MASTER,
                         "force gigabit master") < 0 ||
      mdio_write_bounded(phy, MII_BMCR, BMCR_LOOPBACK | BMCR_SPEED1000 | BMCR_FULLDPLX,
                         "enable PHY loopback") < 0) {
    return -1;
  }
  return 0;
}

// Leave PHY loopback and restart auto-negotiation.
static void phy_disable_loopback_bounded(int phy) {
  (void)mdio_rmw_bounded(phy, MII_CTRL1000, CTRL1000_MANUAL | CTRL1000_MASTER,
                         CTRL1000_FULL, "clear master/slave override");
  (void)mdio_write_bounded(phy, MII_BMCR, BMCR_ANENABLE | BMCR_ANRESTART,
                           "disable PHY loopback");
}

// Find the PHY and configure it for the loopback test.
static int setup_phy_loopback(int *phy_out) {
  uint16_t id1 = 0;
  uint16_t id2 = 0;
  int phy = rtl8211e_find_phy_bounded(&id1, &id2);
  if (phy == -2) {
    return -1;
  }
  if (phy < 0) {
    printf("[phyloop] no RTL8211E on MDIO bus\n");
    return -1;
  }
  printf("[phyloop] RTL8211E at phy=%d id=%04x%04x\n", phy, id1, id2);

  if (rtl8211e_set_rgmii_delay_bounded(phy, /*rx=*/1, /*tx=*/0) < 0 ||
      phy_enable_loopback_bounded(phy) < 0) {
    return -1;
  }
  mdio_delay(kPhySettle);

  uint16_t bmcr = 0;
  uint16_t bmsr = 0;
  uint16_t physr = 0;
  if (mdio_read_bounded(phy, MII_BMCR, &bmcr, "read BMCR") < 0 ||
      mdio_read_bounded(phy, MII_BMSR, &bmsr, "read BMSR") < 0 ||
      mdio_read_bounded(phy, MII_BMSR, &bmsr, "read BMSR latch") < 0 ||
      mdio_read_bounded(phy, MII_PHYSR, &physr, "read PHYSR") < 0) {
    phy_disable_loopback_bounded(phy);
    return -1;
  }
  printf("[phyloop] loopback enabled, bmcr=%04x bmsr=%04x physr=%04x link=%u\n",
         bmcr, bmsr, physr, (bmsr & BMSR_LSTATUS) ? 1u : 0u);
  *phy_out = phy;
  return 0;
}

// Send one Ethernet frame through the MAC with bounded TX waits.
static int eth_send_frame_bounded(const uint8_t *buf, int len) {
  for (int i = 0; i < len; i++) {
    for (long spin = 0; eth_r32(ETH_TX_COUNT) >= ETH_TX_QUEUE_DEPTH; spin++) {
      if (spin >= kTxWaitSpins) {
        printf("[phyloop] timeout: TX queue full before byte %d/%d\n", i, len);
        print_status_snapshot("tx full");
        return -1;
      }
    }
    if (i == len - 1) {
      eth_w32(ETH_TX_LAST, buf[i]);
    } else {
      eth_w32(ETH_TX_DATA, buf[i]);
    }
  }

  for (long spin = 0; eth_r32(ETH_TX_COUNT) != 0; spin++) {
    if (spin >= kTxWaitSpins) {
      printf("[phyloop] timeout: TX queue did not drain after frame write\n");
      print_status_snapshot("tx drain");
      return -1;
    }
  }
  return 0;
}

// Receive one Ethernet frame through the MAC with bounded RX waits.
static int eth_recv_frame_bounded(uint8_t *buf, int max_len, int *rx_len) {
  int n = 0;
  while (true) {
    for (long spin = 0; eth_r32(ETH_RX_COUNT) == 0; spin++) {
      if (spin >= kRxWaitSpins) {
        printf("[phyloop] timeout: RX queue empty while reading byte %d\n", n);
        print_status_snapshot("rx wait");
        return -1;
      }
    }

    uint32_t word = eth_r32(ETH_RX_DATA);
    if (n < max_len) {
      buf[n] = (uint8_t)(word & 0xffu);
    }
    n++;
    if ((word >> 8) & 0x1u) {
      *rx_len = n;
      return 0;
    }
  }
}

// Fill the loopback payload with a deterministic byte pattern.
static void fill_payload(uint8_t payload[kPayloadLen]) {
  for (int i = 0; i < kPayloadLen; i++) {
    payload[i] = (uint8_t)(0xa0 + i);
  }
}

// Build the fixed Ethernet frame used by the PHY loopback test.
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

// Send the deterministic loopback test frame.
static int send_test_frame(const uint8_t payload[kPayloadLen], int *tx_len) {
  *tx_len = build_test_frame(tx_frame, payload);
  printf("[phyloop] tx frame len=%d\n", *tx_len);
  return eth_send_frame_bounded(tx_frame, *tx_len);
}

// Receive the looped-back test frame and capture MAC status.
static int receive_test_frame(int *rx_len, uint32_t *status) {
  if (eth_recv_frame_bounded(rx_frame, (int)sizeof(rx_frame), rx_len) < 0) {
    return -1;
  }
  *status = eth_status();
  printf("[phyloop] rx len=%d status=0x%lx\n", *rx_len, (unsigned long)*status);
  return 0;
}

// Verify a byte range in the received frame.
static int verify_bytes(const char *name, int offset, const uint8_t *expected, int len) {
  for (int i = 0; i < len; i++) {
    if (rx_frame[offset + i] != expected[i]) {
      printf("[phyloop] FAIL %s[%d]=0x%02x exp 0x%02x\n", name, i,
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
    printf("[phyloop] FAIL short rx %d < %d\n", rx_len, tx_len);
    return -1;
  }
  if (verify_bytes("dst", 0, kDstMac, 6) < 0 ||
      verify_bytes("src", 6, kSrcMac, 6) < 0 ||
      verify_bytes("payload", 14, payload, kPayloadLen) < 0) {
    return -1;
  }
  if (rx_frame[12] != (uint8_t)(kEthertype >> 8) ||
      rx_frame[13] != (uint8_t)(kEthertype & 0xffu)) {
    printf("[phyloop] FAIL ethertype 0x%02x%02x\n", rx_frame[12], rx_frame[13]);
    return -1;
  }

  uint32_t error_bits = ETH_ST_RX_ERR_BAD_FRAME | ETH_ST_RX_ERR_BAD_FCS |
                        ETH_ST_RX_FIFO_OVERFLOW;
  if ((status & error_bits) != 0) {
    printf("[phyloop] WARN rx error/overflow bits: 0x%lx\n", (unsigned long)status);
  }
  return 0;
}

// Run one PHY loopback send/receive/verify cycle.
int main(void) {
  setvbuf(stdout, NULL, _IONBF, 0);
  printf("[phyloop] start\n");

  int phy = -1;
  if (setup_phy_loopback(&phy) < 0) {
    printf("LOOPBACK FAIL\n");
    return 1;
  }

  eth_init();
  uint8_t payload[kPayloadLen];
  fill_payload(payload);

  int tx_len = 0;
  int rx_len = 0;
  uint32_t status = 0;
  int ok = send_test_frame(payload, &tx_len) == 0 &&
           receive_test_frame(&rx_len, &status) == 0 &&
           verify_frame(rx_len, tx_len, payload, status) == 0;

  phy_disable_loopback_bounded(phy);

  if (ok) {
    printf("LOOPBACK PASS\n");
    return 0;
  }
  printf("LOOPBACK FAIL\n");
  return 1;
}
