// External (non-inlined) definitions for the inline functions declared in eth.h.

#include "eth.h"

extern void eth_w32(uintptr_t a, uint32_t v);
extern uint32_t eth_r32(uintptr_t a);
extern void eth_init(void);
extern uint32_t eth_status(void);
extern uint32_t eth_link_speed(void);
extern void eth_send_frame(const uint8_t *buf, int len);
extern int eth_recv_frame(uint8_t *buf, int maxlen);
extern int eth_try_recv_frame(uint8_t *buf, int maxlen);
extern int eth_build_frame(uint8_t *buf, const uint8_t dst[6], const uint8_t src[6], uint16_t ethertype, const uint8_t *payload, int plen);
