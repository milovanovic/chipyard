# Build the baremetal Ethernet streaming demo for RocketNexysVideoConfig.
# Mirrors the flags in chipyard's tests/CMakeLists.txt (HTIF / libgloss nano).
#
#   make            # build eth_stream.riscv
#   make dump       # disassembly
#   make clean
#
# Load & run on the FPGA over UART-TSI (from the chipyard env):
#   <path-to>/uart_tsi +tty=/dev/ttyUSB1 ./eth_stream.riscv

PREFIX  ?= riscv64-unknown-elf-
CC      := $(PREFIX)gcc
OBJDUMP := $(PREFIX)objdump

ARCH    := rv64imafd
ABI     := lp64d
CMODEL  := medany

CFLAGS  := -std=gnu99 -O2 -Wall -Wextra -fno-common -fno-builtin-printf \
           -march=$(ARCH) -mabi=$(ABI) -mcmodel=$(CMODEL) -specs=htif_nano.specs
# NB: -specs=htif_nano.specs is in CFLAGS; passing it again here would make the driver
# process the spec file twice ("rename spec 'link'..."), so the link flags omit it.
LDFLAGS := -static -T htif.ld

TARGETS := eth_stream.riscv eth_loopback.riscv eth_phy_loopback.riscv
HDRS    := eth.h mdio.h rtl8211e.h

# Out-of-line definition TUs for the inline functions in the headers. The style guide
# requires exactly one such TU per inline-function header (see eth.c / mdio.c / rtl8211e.c),
# linked into every program that uses the header.
ETH_TU  := eth.c
PHY_TU  := mdio.c rtl8211e.c

all: $(TARGETS)

# Per-target extra source TUs (added to the shared static pattern rule below).
eth_loopback.riscv:     $(ETH_TU)
eth_phy_loopback.riscv: $(ETH_TU) $(PHY_TU)
eth_stream.riscv:       $(ETH_TU) $(PHY_TU)

# Each .riscv target builds from its matching .c plus the shared driver TUs and headers.
$(TARGETS): %.riscv: %.c $(HDRS) htif.ld
	$(CC) $(CFLAGS) $(filter %.c,$^) $(LDFLAGS) -o $@

dump: $(TARGETS)
	$(foreach t,$(TARGETS),$(OBJDUMP) -D $(t) > $(t:.riscv=.dump);)

clean:
	rm -f $(TARGETS) $(TARGETS:.riscv=.dump)

.PHONY: all dump clean
