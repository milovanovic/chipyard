// External (non-inlined) definitions for the inline functions declared in mdio.h.

#include "mdio.h"

extern void mdio_w32(uintptr_t a, uint32_t v);
extern uint32_t mdio_r32(uintptr_t a);
extern uint32_t mdio_pack_cmd(int phy, int reg, uint32_t op);
extern void mdio_wait_idle(void);
extern void mdio_write(int phy, int reg, uint16_t val);
extern uint16_t mdio_read(int phy, int reg);
extern void mdio_rmw(int phy, int reg, uint16_t clear, uint16_t set);
extern void mdio_delay(long iters);
