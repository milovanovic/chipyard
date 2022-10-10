#define SDF_FFT_SIZE 0x2000

#include "mmio.h"
#include <stdio.h>

int main(void)
{
	volatile uint32_t r_size = 0;
	uint32_t w_size = 7;

	reg_write32(SDF_FFT_SIZE, w_size);
	r_size = reg_read32(SDF_FFT_SIZE);

	if (r_size != w_size) {
      printf("FAIL: Read value and writen value differed: %d %d", r_size, w_size);
      return -1;
    }

	printf("PASS: SDF-FFT Test Passed\n");
	return 0;
}
