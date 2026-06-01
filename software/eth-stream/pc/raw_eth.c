// PC-side raw L2 Ethernet tool for the RocketNexysVideoConfig RGMII demo.
//
// The SoC has no IP/ARP stack.
// It sends and receives raw Ethernet frames with a custom EtherType (0x88B5). 
// This tool uses an AF_PACKET/SOCK_RAW socket to send/receive those frames directly on a NIC.
//
// Build:  make (in this directory)
// Usage:  sudo ./raw_eth rx <ifname>            # receive & print SoC frames
//         sudo ./raw_eth tx <ifname> [count]    # send `count` frames to the SoC
//
// Tip: first smoke test without this tool:
//   sudo tcpdump -i <ifname> -e -XX ether proto 0x88b5
//
// Notes:
//   - Run as root.
//   - Make sure the link is up at 1 Gbps and offloads don't mangle tiny raw frames:
//       sudo ethtool -K <ifname> rx off tx off gro off gso off tso off
//   - <ifname> is the PC NIC the Ethernet cable from the board plugs into (e.g. enp3s0).

#include <arpa/inet.h>
#include <linux/if_packet.h>
#include <net/ethernet.h>
#include <net/if.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <unistd.h>

enum {
  kEthertype  = 0x88B5,
  kPayloadLen = 46,
};

static int open_raw(const char *ifname, int *ifindex, unsigned char *mac) {
  int fd = socket(AF_PACKET, SOCK_RAW, htons(kEthertype));
  if (fd < 0) { perror("socket"); return -1; }

  struct ifreq ifr;
  memset(&ifr, 0, sizeof(ifr));
  strncpy(ifr.ifr_name, ifname, IFNAMSIZ - 1);
  if (ioctl(fd, SIOCGIFINDEX, &ifr) < 0) { perror("SIOCGIFINDEX"); close(fd); return -1; }
  *ifindex = ifr.ifr_ifindex;
  if (ioctl(fd, SIOCGIFHWADDR, &ifr) < 0) { perror("SIOCGIFHWADDR"); close(fd); return -1; }
  memcpy(mac, ifr.ifr_hwaddr.sa_data, 6);
  return fd;
}

static int do_rx(const char *ifname) {
  int ifindex;
  unsigned char mac[6];
  int fd = open_raw(ifname, &ifindex, mac);
  if (fd < 0) return 1;

  printf("[pc] listening on %s for EtherType 0x%04x\n", ifname, kEthertype);
  unsigned char buf[2048];
  unsigned long n = 0;
  while (true) {
    ssize_t len = recv(fd, buf, sizeof(buf), 0);
    if (len < 0) {
      perror("recv");
      break;
    }
    if (len < 14) {
      continue;
    }
    unsigned seq = 0;
    if (len >= 18) {
      seq = (buf[14] << 24) | (buf[15] << 16) | (buf[16] << 8) | buf[17];
    }
    printf("[pc] rx #%lu len=%zd src=%02x:%02x:%02x:%02x:%02x:%02x seq=%u\n",
           n++, len, buf[6], buf[7], buf[8], buf[9], buf[10], buf[11], seq);
  }
  close(fd);
  return 0;
}

static int do_tx(const char *ifname, int count) {
  int ifindex;
  unsigned char mac[6];
  int fd = open_raw(ifname, &ifindex, mac);
  if (fd < 0) return 1;

  // Destination = the SoC MAC chosen in eth_stream.c (02:00:00:00:00:01).
  unsigned char dst[6] = {0x02, 0x00, 0x00, 0x00, 0x00, 0x01};

  struct sockaddr_ll sa;
  memset(&sa, 0, sizeof(sa));
  sa.sll_family = AF_PACKET;
  sa.sll_ifindex = ifindex;
  sa.sll_halen = 6;
  memcpy(sa.sll_addr, dst, 6);

  unsigned char frame[14 + kPayloadLen];
  for (int seq = 0; seq < count; seq++) {
    memset(frame, 0, sizeof(frame));
    memcpy(frame, dst, 6);
    memcpy(frame + 6, mac, 6);
    frame[12] = (kEthertype >> 8) & 0xFF;
    frame[13] = kEthertype & 0xFF;
    frame[14] = (seq >> 24) & 0xFF;
    frame[15] = (seq >> 16) & 0xFF;
    frame[16] = (seq >> 8) & 0xFF;
    frame[17] = seq & 0xFF;
    for (int i = 18; i < 14 + kPayloadLen; i++) {
      frame[i] = i;
    }

    ssize_t s = sendto(fd, frame, sizeof(frame), 0, (struct sockaddr *)&sa, sizeof(sa));
    if (s < 0) {
      perror("sendto");
      close(fd);
      return 1;
    }
    printf("[pc] tx seq=%d len=%zd\n", seq, s);
  }
  close(fd);
  return 0;
}

int main(int argc, char **argv) {
  if (argc < 3) {
    fprintf(stderr, "usage: %s rx <ifname>\n       %s tx <ifname> [count]\n",
            argv[0], argv[0]);
    return 2;
  }
  if (strcmp(argv[1], "rx") == 0) return do_rx(argv[2]);
  if (strcmp(argv[1], "tx") == 0) return do_tx(argv[2], argc > 3 ? atoi(argv[3]) : 16);
  fprintf(stderr, "unknown mode '%s'\n", argv[1]);
  return 2;
}
