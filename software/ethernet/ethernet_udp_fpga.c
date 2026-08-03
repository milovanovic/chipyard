#include "software/ethernet/eth_udp.h"
#include "software/ethernet/ethernet_udp_file_echo.h"
#include "software/ethernet/rtl8211e.h"

#include <stdbool.h>
#include <stdio.h>

int main(void) {
  const eth_udp_config_t config = {
      .local_mac = UINT64_C(0x020000000001),
      .local_ip = UINT32_C(0xc0a80180),
      .gateway_ip = UINT32_C(0xc0a80101),
      .subnet_mask = UINT32_C(0xffffff00),
  };
  setvbuf(stdout, NULL, _IONBF, 0);

  printf("[ethernet-udp] PHY and MAC initialization\n");
  (void)rtl8211e_bringup(printf);
  if (eth_udp_init(&config) < 0) {
    printf("[ethernet-udp] initialization failed\n");
    return 1;
  }

  while (true) {
    if (ethernet_udp_file_echo_once(true) < 0) {
      printf("[ethernet-udp] transfer failed; waiting for next transfer\n");
    }
  }
}
