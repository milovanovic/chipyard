CC     ?= cc
CFLAGS ?= -O2 -Wall -Wextra

all: raw_eth

raw_eth: raw_eth.c
	$(CC) $(CFLAGS) raw_eth.c -o raw_eth

clean:
	rm -f raw_eth

.PHONY: all clean
