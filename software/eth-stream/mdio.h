// Baremetal driver for the rivet MMIO MDIO master (rivet.wrapper.WithEthernetMDIO).
//
// Lets software read/write an Ethernet PHY's management registers over MDC/MDIO.
// This header is the generic MDIO-bus layer; PHY-specific logic (e.g. the Realtek
// RTL8211E on the Nexys Video) lives in `rtl8211e.h`, which includes this file.
//
// Register map (PBUS absolute addresses, see mdio.scala):
//   0x2900 +0x00 (W,16): cmd_data  -- data for the next WRITE (write before triggering)
//          +0x04 (W,12): trigger   -- {reg[4:0],phy[4:0],op[1:0]}; the write starts the txn
//          +0x08 (R)   : status    -- bit0 busy, bit1 read-data valid
//          +0x0C (R,16): rdata     -- result of the last READ (reading pops it)
//          +0x10 (W,8) : prescale  -- MDC = clk/(2*(prescale+1)); default 0xFF

#ifndef RIVET_MDIO_H
#define RIVET_MDIO_H

#include <stdint.h>

/**
 * Write a 32-bit word to MMIO address `a`.
 *
 * @param a MMIO address.
 * @param v Value to write.
 */
inline void mdio_w32(uintptr_t a, uint32_t v) { *(volatile uint32_t *)a = v; }

/**
 * Read a 32-bit word from MMIO address `a`.
 *
 * @param a MMIO address.
 * @return The value read.
 */
inline uint32_t mdio_r32(uintptr_t a) { return *(volatile uint32_t *)a; }

#define MDIO_BASE   0x2900UL
#define MDIO_WDATA  (MDIO_BASE + 0x00)
#define MDIO_CMD    (MDIO_BASE + 0x04)
#define MDIO_STAT   (MDIO_BASE + 0x08)
#define MDIO_RDATA  (MDIO_BASE + 0x0C)
#define MDIO_PRESC  (MDIO_BASE + 0x10)

#define MDIO_STAT_BUSY   (1u << 0)
#define MDIO_STAT_RVALID (1u << 1)

// Frame opcode (OP field): 0b10 = read, 0b01 = write (see mdio_master.v).
#define MDIO_OP_WRITE 0x1u
#define MDIO_OP_READ  0x2u

/**
 * Pack a `{reg, phy, op}` command word for the MDIO_CMD trigger register.
 *
 * @param phy PHY address (5 bits).
 * @param reg Register address (5 bits).
 * @param op  Opcode (`MDIO_OP_READ` / `MDIO_OP_WRITE`).
 * @return The packed command word.
 */
inline uint32_t mdio_pack_cmd(int phy, int reg, uint32_t op) {
  return ((uint32_t)(reg & 0x1F) << 7) | ((uint32_t)(phy & 0x1F) << 2) | (op & 0x3);
}

/**
 * Spin until the MDIO master is idle (no transaction in flight).
 */
inline void mdio_wait_idle(void) {
  while (mdio_r32(MDIO_STAT) & MDIO_STAT_BUSY) {
    // spin
  }
}

/**
 * Write one PHY register over MDIO.
 *
 * @param phy PHY address.
 * @param reg Register address.
 * @param val 16-bit value to write.
 */
inline void mdio_write(int phy, int reg, uint16_t val) {
  mdio_wait_idle();
  mdio_w32(MDIO_WDATA, val);
  mdio_w32(MDIO_CMD, mdio_pack_cmd(phy, reg, MDIO_OP_WRITE));
  mdio_wait_idle();
}

/**
 * Read one PHY register over MDIO.
 *
 * @param phy PHY address.
 * @param reg Register address.
 * @return The 16-bit register value.
 */
inline uint16_t mdio_read(int phy, int reg) {
  mdio_wait_idle();
  mdio_w32(MDIO_CMD, mdio_pack_cmd(phy, reg, MDIO_OP_READ));
  mdio_wait_idle();
  return (uint16_t)(mdio_r32(MDIO_RDATA) & 0xFFFF);
}

/**
 * Read/modify/write helper for a PHY register.
 *
 * @param phy   PHY address.
 * @param reg   Register address.
 * @param clear Bits to clear.
 * @param set   Bits to set.
 */
inline void mdio_rmw(int phy, int reg, uint16_t clear, uint16_t set) {
  uint16_t v = mdio_read(phy, reg);
  v = (uint16_t)((v & ~clear) | set);
  mdio_write(phy, reg, v);
}

/**
 * Crude busy-wait spin loop.
 *
 * Useful for letting a PHY settle (e.g. bring up an internal loopback datapath/PLL).
 *
 * @param iters Number of spin iterations.
 */
inline void mdio_delay(long iters) {
  for (volatile long i = 0; i < iters; i++) {
    // spin
  }
}

#endif // RIVET_MDIO_H
