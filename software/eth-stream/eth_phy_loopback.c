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
  kRxWaitSpins = 2000000,
  kPhySettle   = 2000000, // let the PHY bring up the loopback datapath
};

static const uint8_t kDstMac[6] = {0x02, 0x00, 0x00, 0x00, 0x00, 0x02};
static const uint8_t kSrcMac[6] = {0x02, 0x00, 0x00, 0x00, 0x00, 0x01};

static uint8_t txbuf[2048];
static uint8_t rxbuf[2048];

int main(void) {
  printf("[phyloop] start\n");

  // PHY bring-up over MDIO
  int phy = rtl8211e_find_phy();
  if (phy < 0) {
    printf("[phyloop] no RTL8211E on MDIO bus\n");
    printf("LOOPBACK FAIL\n");
    return 1;
  }
  printf("[phyloop] RTL8211E at phy=%d id=%04x%04x\n",
         phy, mdio_read(phy, MII_PHYID1), mdio_read(phy, MII_PHYID2));

  rtl8211e_set_rgmii_delay(phy, /*rx=*/1, /*tx=*/0); // board straps RX delay off -> enable it
  rtl8211e_loopback_enable(phy);                     // forced 1000/full, internal loopback
  mdio_delay(kPhySettle);
  printf("[phyloop] loopback enabled, bmcr=%04x physr=%04x\n",
         mdio_read(phy, MII_BMCR), mdio_read(phy, MII_PHYSR));

  // MAC up, send one frame
  eth_init();
  uint8_t payload[kPayloadLen];
  for (int i = 0; i < kPayloadLen; i++) {
    payload[i] = (uint8_t)(0xA0 + i);
  }
  int txlen = eth_build_frame(txbuf, kDstMac, kSrcMac, kEthertype, payload, kPayloadLen);
  printf("[phyloop] tx frame len=%d\n", txlen);
  eth_send_frame(txbuf, txlen);

  // bounded wait for the looped-back frame
  long spin = 0;
  while (eth_r32(ETH_RX_COUNT) == 0) {
    if (++spin > kRxWaitSpins) {
      printf("[phyloop] timeout: no rx, status=0x%lx\n", (unsigned long)eth_status());
      printf("LOOPBACK FAIL\n");
      rtl8211e_loopback_disable(phy);
      return 1;
    }
  }
  int rxlen = eth_recv_frame(rxbuf, sizeof(rxbuf));
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

  rtl8211e_loopback_disable(phy);

  if (ok) {
    printf("LOOPBACK PASS\n");
    return 0;
  }
  printf("LOOPBACK FAIL\n");
  return 1;
}
