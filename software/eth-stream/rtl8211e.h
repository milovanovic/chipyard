// Realtek RTL8211E PHY driver (on the Nexys Video) over the MDIO bus.
//
// Confirm PHY ID, read link/speed, enable RGMII internal RX/TX delay, drive internal (near-end) loopback, and restart auto-negotiation.

#ifndef RIVET_RTL8211E_H
#define RIVET_RTL8211E_H

#include "mdio.h"
#include <stdint.h>

// Standard MII register addresses and bit definitions, plus a few RTL8211E-specific ones.
#define MII_BMCR     0x00 // basic control
#define MII_BMSR     0x01 // basic status
#define MII_PHYID1   0x02
#define MII_PHYID2   0x03
#define MII_CTRL1000 0x09 // 1000BASE-T control (master/slave config)
#define MII_PHYSR    0x11 // RTL8211x PHY-specific status

#define BMCR_RESET     0x8000
#define BMCR_LOOPBACK  0x4000 // internal (near-end) loopback
#define BMCR_ANENABLE  0x1000
#define BMCR_FULLDPLX  0x0100
#define BMCR_SPEED1000 0x0040 // {bit6,bit13}=10 -> 1000 Mbps
#define BMCR_ANRESTART 0x0200
#define BMSR_LSTATUS   0x0004 // link up
#define BMSR_ANEGCOMP  0x0020 // auto-neg complete

// 1000BASE-T control (reg 9): force manual master/slave. Needed for gigabit loopback, which has no link partner to negotiate clock master with.
#define CTRL1000_MANUAL  0x1000 // enable manual master/slave config
#define CTRL1000_MASTER  0x0800 // prefer master

// RTL8211E identity: PHYID1 = 0x001C, PHYID2 = 0xC915 (low nibble = silicon revision).
#define RTL8211E_ID1      0x001C
#define RTL8211E_ID2_MASK 0xFFF0
#define RTL8211E_ID2      0xC910

// Default PHY address on the Nexys Video (PHY_AD[2:0] straps = 00001).
#define RTL8211E_DEFAULT_PHYAD 1

// RTL8211E extension-page access for RGMII internal delay.
// Page-select reg 0x1f = 0x07 enters extension mode,
// reg 0x1e selects extension page 0xa4, then reg 0x1c holds the delay bits.
// Restore page 0.
#define RTL_PAGE_SELECT        0x1f
#define RTL_EXT_PAGE_SELECT    0x1e
#define RTL8211E_EXT_PAGE      0xa4
#define RTL8211E_RGMII_CFG_REG 0x1c
#define RTL8211E_TX_DELAY      (1u << 1) // ~2 ns on TXC
#define RTL8211E_RX_DELAY      (1u << 2) // ~2 ns on RXC

/**
 * Scan the MDIO bus for an RTL8211E.
 *
 * @return The PHY address, or -1 if not found.
 */
inline int rtl8211e_find_phy(void) {
  for (int phy = 0; phy < 32; phy++) {
    uint16_t id1 = mdio_read(phy, MII_PHYID1);
    uint16_t id2 = mdio_read(phy, MII_PHYID2);
    if (id1 == RTL8211E_ID1 && (id2 & RTL8211E_ID2_MASK) == RTL8211E_ID2) {
      return phy;
    }
  }
  return -1;
}

/**
 * Enable RGMII internal delay on the RTL8211E.
 *
 * Defaults to RX-only ("rgmii-rxid"): enable the ~2 ns RXC delay the FPGA needs, but leave TX delay OFF, 
 * because the Forencich MAC already center-aligns `rgmii_tx_clk` via `gtx_clk90` (FPGA-side TX skew). Enabling the PHY TX delay too would double it and overshoot spec.
 *
 * @param phy PHY address.
 * @param rx  Nonzero to enable the RXC delay.
 * @param tx  Nonzero to enable the TXC delay.
 */
inline void rtl8211e_set_rgmii_delay(int phy, int rx, int tx) {
  uint16_t set = (uint16_t)((rx ? RTL8211E_RX_DELAY : 0) | (tx ? RTL8211E_TX_DELAY : 0));
  mdio_write(phy, RTL_PAGE_SELECT, 0x0007); // enter extension-page mode
  mdio_write(phy, RTL_EXT_PAGE_SELECT, RTL8211E_EXT_PAGE);
  mdio_rmw(phy, RTL8211E_RGMII_CFG_REG, RTL8211E_RX_DELAY | RTL8211E_TX_DELAY, set);
  mdio_write(phy, RTL_PAGE_SELECT, 0x0000); // restore page 0
}

/**
 * Report PHY link status.
 *
 * @param phy PHY address.
 * @return 1 if link is up, 0 otherwise.
 */
inline int rtl8211e_link_up(int phy) {
  (void)mdio_read(phy, MII_BMSR); // BMSR latches low; read twice for live state
  return (mdio_read(phy, MII_BMSR) & BMSR_LSTATUS) ? 1 : 0;
}

/**
 * Report link speed from the PHY-specific status register.
 *
 * @param phy PHY address.
 * @return 0 = 10M, 1 = 100M, 2 = 1G.
 */
inline int rtl8211e_speed(int phy) {
  return (mdio_read(phy, MII_PHYSR) >> 14) & 0x3;
}

/**
 * Restart auto-negotiation.
 *
 * @param phy PHY address.
 */
inline void rtl8211e_restart_aneg(int phy) {
  mdio_rmw(phy, MII_BMCR, 0, BMCR_ANENABLE | BMCR_ANRESTART);
}

/**
 * Put the PHY into near-end (internal) loopback at forced 1000/full.
 *
 * Data the FPGA sends out RGMII TX loops back inside the PHY and returns on RGMII RX, exercising the real RGMII pad timing + RX delay.
 * Gigabit loopback has no peer to negotiate clock master with, so we force manual master first.
 *
 * @param phy PHY address.
 */
inline void rtl8211e_loopback_enable(int phy) {
  mdio_write(phy, MII_CTRL1000, CTRL1000_MANUAL | CTRL1000_MASTER);
  mdio_write(phy, MII_BMCR, BMCR_LOOPBACK | BMCR_SPEED1000 | BMCR_FULLDPLX);
}

/**
 * Leave loopback: clear it, restore auto master/slave, and restart auto-negotiation.
 *
 * @param phy PHY address.
 */
inline void rtl8211e_loopback_disable(int phy) {
  mdio_write(phy, MII_CTRL1000, 0x0000);
  mdio_write(phy, MII_BMCR, BMCR_ANENABLE | BMCR_ANRESTART);
}

/**
 * One-shot bring-up: find the PHY, print identity/link/speed, enable RGMII delay, kick auto-negotiation.
 *
 * @param print A printf-style callback used for status output (may be NULL).
 * @return The PHY address (>= 0) on success, -1 if no PHY found.
 */
inline int rtl8211e_bringup(int (*print)(const char *, ...)) {
  int phy = rtl8211e_find_phy();
  if (phy < 0) {
    if (print) {
      print("[mdio] no RTL8211E found on the MDIO bus\n");
    }
    return -1;
  }
  uint16_t id1 = mdio_read(phy, MII_PHYID1);
  uint16_t id2 = mdio_read(phy, MII_PHYID2);
  if (print) {
    print("[mdio] RTL8211E at phy=%d id=%04x%04x\n", phy, id1, id2);
  }

  rtl8211e_set_rgmii_delay(phy, /*rx=*/1, /*tx=*/0); // enable RX delay (board straps it off)
  rtl8211e_restart_aneg(phy);

  if (print) {
    print("[mdio] link=%s speed=%d (0=10M,1=100M,2=1G)\n",
          rtl8211e_link_up(phy) ? "up" : "down", rtl8211e_speed(phy));
  }
  return phy;
}

#endif // RIVET_RTL8211E_H
