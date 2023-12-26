# ------------------------- Base Clocks --------------------
create_clock -name sys_clock -period 10.0 [get_ports {sys_clock}]
set_input_jitter sys_clock 0.5
create_clock -name rgmii_rxc -period 8.0 [get_ports {top_rgmii_rxc}]
set_input_jitter rgmii_rxc 0.5
# ------------------------- Clock Groups -------------------
set_clock_groups -asynchronous \
  -group [list [get_clocks -of_objects [get_pins { \
      harnessSysPLLNode/clk_out1 \
    }]]] \
  -group [list [get_clocks { \
      rgmii_rxc \
    }]]
# ------------------------- False Paths --------------------
set_false_path -through [get_pins {powerOnReset_fpga_power_on/power_on_reset}]
# ------------------------- IO Timings ---------------------

set_property CLOCK_DEDICATED_ROUTE FALSE [get_nets chiptop0/system/top/top/lvds_rx_1/pll_lvds/inst/clk_in1_PLL_LVDS]
