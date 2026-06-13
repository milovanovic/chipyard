// On-board RGMII loopback self-test using the RTL8211E's internal loopback.
//
// Unlike eth_loopback.c (which relies on a testbench wiring RGMII TX->RX in simulation),
// this runs on REAL hardware with no cable and no link partner.
// It uses MDIO to put the PHY into internal loopback, so a frame the FPGA transmits loops back inside the PHY and returns on RGMII RX.
//
// Flow:
//   1. init MDIO
//   2. find PHY
//   3. enable RGMII RX delay
//   4. enable PHY loopback
//   5. enable MAC
//   6. send a frame
//   7. wait (bounded) for it back
//   8. verify header+payload+`last`
//   9. report
//
// Run on the board over UART-TSI:  uart_tsi +tty=/dev/ttyUSB* ./eth_phy_loopback.riscv

#include "eth.h"
#include "rtl8211e.h"
#include <stdint.h>
#include <stdio.h>
#include <string.h>

enum {
  kEthertype  = 0x88B5,
  kPayloadLen = 46, // 14 + 46 = 60 B frame; MAC appends 4 B FCS -> 64 B
  kMdioWaitSpins = 2000000,
  kTxWaitSpins   = 2000000,
  kRxWaitSpins   = 2000000,
  kPhySettle     = 2000000, // let the PHY bring up the loopback datapath
};

static const uint8_t kDstMac[6] = {0x02, 0x00, 0x00, 0x00, 0x00, 0x02};
static const uint8_t kSrcMac[6] = {0x02, 0x00, 0x00, 0x00, 0x00, 0x01};

static uint8_t txbuf[2048];
static uint8_t rxbuf[2048];

static void print_status_snapshot(const char *where) {
  printf("[phyloop] %s: mdio_stat=0x%lx tx_count=%lu rx_count=%lu eth_status=0x%lx\n",
         where,
         (unsigned long)mdio_r32(MDIO_STAT),
         (unsigned long)eth_r32(ETH_TX_COUNT),
         (unsigned long)eth_r32(ETH_RX_COUNT),
         (unsigned long)eth_status());
}

static int mdio_wait_idle_bounded(const char *where) {
  for (long spin = 0; spin < kMdioWaitSpins; spin++) {
    uint32_t stat = mdio_r32(MDIO_STAT);
    if ((stat & MDIO_STAT_BUSY) == 0) {
      return 0;
    }
  }
  printf("[phyloop] timeout: MDIO busy during %s\n", where);
  print_status_snapshot("mdio timeout");
  return -1;
}

static int mdio_read_bounded(int phy, int reg, uint16_t *val, const char *where) {
  if (mdio_wait_idle_bounded(where) < 0) {
    return -1;
  }
  mdio_w32(MDIO_CMD, mdio_pack_cmd(phy, reg, MDIO_OP_READ));
  if (mdio_wait_idle_bounded(where) < 0) {
    return -1;
  }
  *val = (uint16_t)(mdio_r32(MDIO_RDATA) & 0xFFFF);
  return 0;
}

static int mdio_write_bounded(int phy, int reg, uint16_t val, const char *where) {
  if (mdio_wait_idle_bounded(where) < 0) {
    return -1;
  }
  mdio_w32(MDIO_WDATA, val);
  mdio_w32(MDIO_CMD, mdio_pack_cmd(phy, reg, MDIO_OP_WRITE));
  return mdio_wait_idle_bounded(where);
}

static int mdio_rmw_bounded(int phy, int reg, uint16_t clear, uint16_t set, const char *where) {
  uint16_t val = 0;
  if (mdio_read_bounded(phy, reg, &val, where) < 0) {
    return -1;
  }
  val = (uint16_t)((val & ~clear) | set);
  return mdio_write_bounded(phy, reg, val, where);
}

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

static int rtl8211e_set_rgmii_delay_bounded(int phy, int rx, int tx) {
  uint16_t set = (uint16_t)((rx ? RTL8211E_RX_DELAY : 0) | (tx ? RTL8211E_TX_DELAY : 0));
  if (mdio_write_bounded(phy, RTL_PAGE_SELECT, 0x0007, "select ext page mode") < 0 ||
      mdio_write_bounded(phy, RTL_EXT_PAGE_SELECT, RTL8211E_EXT_PAGE, "select RGMII page") < 0 ||
      mdio_rmw_bounded(phy, RTL8211E_RGMII_CFG_REG,
                        RTL8211E_RX_DELAY | RTL8211E_TX_DELAY, set, "set RGMII delay") < 0 ||
      mdio_write_bounded(phy, RTL_PAGE_SELECT, 0x0000, "restore page 0") < 0) {
    return -1;
  }
  return 0;
}

static int rtl8211e_loopback_enable_bounded(int phy) {
  if (mdio_write_bounded(phy, MII_CTRL1000, CTRL1000_MANUAL | CTRL1000_MASTER,
                         "force gigabit master") < 0 ||
      mdio_write_bounded(phy, MII_BMCR, BMCR_LOOPBACK | BMCR_SPEED1000 | BMCR_FULLDPLX,
                         "enable PHY loopback") < 0) {
    return -1;
  }
  return 0;
}

static void rtl8211e_loopback_disable_bounded(int phy) {
  (void)mdio_rmw_bounded(phy, MII_CTRL1000, CTRL1000_MANUAL | CTRL1000_MASTER, CTRL1000_FULL,
                         "clear master/slave override");
  (void)mdio_write_bounded(phy, MII_BMCR, BMCR_ANENABLE | BMCR_ANRESTART,
                           "disable PHY loopback");
}

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

static int eth_recv_frame_bounded(uint8_t *buf, int maxlen, int *rxlen) {
  int n = 0;
  while (1) {
    for (long spin = 0; eth_r32(ETH_RX_COUNT) == 0; spin++) {
      if (spin >= kRxWaitSpins) {
        printf("[phyloop] timeout: RX queue empty while reading byte %d\n", n);
        print_status_snapshot("rx wait");
        return -1;
      }
    }

    uint32_t w = eth_r32(ETH_RX_DATA);
    if (n < maxlen) {
      buf[n] = (uint8_t)(w & 0xFF);
    } else {
      printf("[phyloop] timeout: RX frame exceeded buffer without last\n");
      print_status_snapshot("rx too long");
      return -1;
    }
    n++;

    if ((w >> 8) & 0x1) {
      *rxlen = n;
      return 0;
    }
  }
}

int main(void) {
  setvbuf(stdout, NULL, _IONBF, 0);
  printf("[phyloop] start\n");

  // PHY bring-up over MDIO
  uint16_t id1 = 0;
  uint16_t id2 = 0;
  int phy = rtl8211e_find_phy_bounded(&id1, &id2);
  if (phy == -2) {
    printf("LOOPBACK FAIL\n");
    return 1;
  }
  if (phy < 0) {
    printf("[phyloop] no RTL8211E on MDIO bus\n");
    printf("LOOPBACK FAIL\n");
    return 1;
  }
  printf("[phyloop] RTL8211E at phy=%d id=%04x%04x\n", phy, id1, id2);

  if (rtl8211e_set_rgmii_delay_bounded(phy, /*rx=*/1, /*tx=*/0) < 0 ||
      rtl8211e_loopback_enable_bounded(phy) < 0) {
    printf("LOOPBACK FAIL\n");
    return 1;
  }
  mdio_delay(kPhySettle);

  uint16_t bmcr = 0;
  uint16_t physr = 0;
  if (mdio_read_bounded(phy, MII_BMCR, &bmcr, "read BMCR") < 0 ||
      mdio_read_bounded(phy, MII_PHYSR, &physr, "read PHYSR") < 0) {
    printf("LOOPBACK FAIL\n");
    rtl8211e_loopback_disable_bounded(phy);
    return 1;
  }
  printf("[phyloop] loopback enabled, bmcr=%04x physr=%04x\n",
         bmcr, physr);

  // MAC up, send one frame
  eth_init();
  uint8_t payload[kPayloadLen];
  for (int i = 0; i < kPayloadLen; i++) {
    payload[i] = (uint8_t)(0xA0 + i);
  }
  int txlen = eth_build_frame(txbuf, kDstMac, kSrcMac, kEthertype, payload, kPayloadLen);
  printf("[phyloop] tx frame len=%d\n", txlen);
  if (eth_send_frame_bounded(txbuf, txlen) < 0) {
    printf("LOOPBACK FAIL\n");
    rtl8211e_loopback_disable_bounded(phy);
    return 1;
  }

  // bounded wait for the looped-back frame
  long spin = 0;
  while (eth_r32(ETH_RX_COUNT) == 0) {
    if (++spin > kRxWaitSpins) {
      printf("[phyloop] timeout: no rx\n");
      print_status_snapshot("rx timeout");
      printf("LOOPBACK FAIL\n");
      rtl8211e_loopback_disable_bounded(phy);
      return 1;
    }
  }
  int rxlen = 0;
  if (eth_recv_frame_bounded(rxbuf, sizeof(rxbuf), &rxlen) < 0) {
    printf("LOOPBACK FAIL\n");
    rtl8211e_loopback_disable_bounded(phy);
    return 1;
  }
  uint32_t st = eth_status();
  printf("[phyloop] rx len=%d status=0x%lx\n", rxlen, (unsigned long)st);

  // verify (tolerant to FCS strip / padding)
  int ok = 1;
  if (rxlen < txlen) {
    printf("[phyloop] FAIL short rx %d < %d\n", rxlen, txlen);
    ok = 0;
  }
  for (int i = 0; ok && i < 6; i++) {
    if (rxbuf[i] != kDstMac[i]) {
      printf("[phyloop] FAIL dst[%d]\n", i);
      ok = 0;
    }
  }
  for (int i = 0; ok && i < 6; i++) {
    if (rxbuf[6 + i] != kSrcMac[i]) {
      printf("[phyloop] FAIL src[%d]\n", i);
      ok = 0;
    }
  }
  if (ok && (rxbuf[12] != (uint8_t)(kEthertype >> 8) || rxbuf[13] != (uint8_t)(kEthertype & 0xFF))) {
    printf("[phyloop] FAIL ethertype 0x%02x%02x\n", rxbuf[12], rxbuf[13]);
    ok = 0;
  }
  for (int i = 0; ok && i < kPayloadLen; i++) {
    if (rxbuf[14 + i] != payload[i]) {
      printf("[phyloop] FAIL payload[%d]=0x%02x exp 0x%02x\n", i, rxbuf[14 + i], payload[i]);
      ok = 0;
    }
  }
  if (st & (ETH_ST_RX_ERR_BAD_FRAME | ETH_ST_RX_ERR_BAD_FCS | ETH_ST_RX_FIFO_OVERFLOW)) {
    printf("[phyloop] WARN rx error/overflow bits: 0x%lx\n", (unsigned long)st);
  }

  rtl8211e_loopback_disable_bounded(phy);

  if (ok) {
    printf("LOOPBACK PASS\n");
    return 0;
  }
  printf("LOOPBACK FAIL\n");
  return 1;
}
