// Shared receive/store/echo service for the raw-Ethernet file-transfer protocol.

#ifndef SOFTWARE_ETHERNET_ETHERNET_FILE_ECHO_H_
#define SOFTWARE_ETHERNET_ETHERNET_FILE_ECHO_H_

#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Receive one file transfer and echo it to the sender.
 *
 * The Ethernet MAC and DMA must be initialized before this function is called.
 *
 * @param verbose Whether to print transfer progress and protocol diagnostics.
 * @return 0 after a successful echoed transfer, or -1 after a protocol error.
 */
int ethernet_file_echo_once(bool verbose);

#ifdef __cplusplus
}  // extern "C"
#endif

#endif  // SOFTWARE_ETHERNET_ETHERNET_FILE_ECHO_H_
