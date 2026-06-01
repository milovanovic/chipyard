// External (non-inlined) definitions for the inline functions declared in rtl8211e.h.

#include "rtl8211e.h"

extern int rtl8211e_find_phy(void);
extern void rtl8211e_set_rgmii_delay(int phy, int rx, int tx);
extern int rtl8211e_link_up(int phy);
extern int rtl8211e_speed(int phy);
extern void rtl8211e_restart_aneg(int phy);
extern void rtl8211e_loopback_enable(int phy);
extern void rtl8211e_loopback_disable(int phy);
extern int rtl8211e_bringup(int (*print)(const char *, ...));
