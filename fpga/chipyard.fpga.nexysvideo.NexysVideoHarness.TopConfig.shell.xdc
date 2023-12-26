set_property PACKAGE_PIN {R4} [get_ports {sys_clock}]
set_property IOSTANDARD {LVCMOS33} [get_ports {sys_clock}]
set_property PACKAGE_PIN {V18} [get_ports {uart_rxd}]
set_property IOSTANDARD {LVCMOS33} [get_ports {uart_rxd}]
set_property IOB {TRUE} [ get_cells -of_objects [ all_fanin -flat -startpoints_only [get_ports {uart_rxd}]]]
set_property PACKAGE_PIN {AA19} [get_ports {uart_txd}]
set_property IOSTANDARD {LVCMOS33} [get_ports {uart_txd}]
set_property IOB {TRUE} [ get_cells -of_objects [ all_fanin -flat -startpoints_only [get_ports {uart_txd}]]]


set_property -dict { PACKAGE_PIN U7    IOSTANDARD LVCMOS33 } [get_ports { top_phy_resetn }]; #IO_25_34 Sch=eth_rst_b


set_property -dict { PACKAGE_PIN AA16  IOSTANDARD LVCMOS25 } [get_ports { top_mdc }]; #IO_L1N_T0_13 Sch=eth_mdc
set_property -dict { PACKAGE_PIN Y16   IOSTANDARD LVCMOS25 } [get_ports { top_mdio }]; #IO_L1P_T0_13 Sch=eth_mdio

set_property -dict { PACKAGE_PIN AB16  IOSTANDARD LVCMOS25 } [get_ports { top_rgmii_rxd[0] }]; #IO_L2P_T0_13 Sch=eth_rxd[0]
set_property -dict { PACKAGE_PIN AA15  IOSTANDARD LVCMOS25 } [get_ports { top_rgmii_rxd[1] }]; #IO_L4P_T0_13 Sch=eth_rxd[1]
set_property -dict { PACKAGE_PIN AB15  IOSTANDARD LVCMOS25 } [get_ports { top_rgmii_rxd[2] }]; #IO_L4N_T0_13 Sch=eth_rxd[2]
set_property -dict { PACKAGE_PIN AB11  IOSTANDARD LVCMOS25 } [get_ports { top_rgmii_rxd[3] }]; #IO_L7P_T1_13 Sch=eth_rxd[3]

set_property -dict { PACKAGE_PIN Y12   IOSTANDARD LVCMOS25 } [get_ports { top_rgmii_txd[0] }]; #IO_L11N_T1_SRCC_13 Sch=eth_txd[0]
set_property -dict { PACKAGE_PIN W12   IOSTANDARD LVCMOS25 } [get_ports { top_rgmii_txd[1] }]; #IO_L12N_T1_MRCC_13 Sch=eth_txd[1]
set_property -dict { PACKAGE_PIN W11   IOSTANDARD LVCMOS25 } [get_ports { top_rgmii_txd[2] }]; #IO_L12P_T1_MRCC_13 Sch=eth_txd[2]
set_property -dict { PACKAGE_PIN Y11   IOSTANDARD LVCMOS25 } [get_ports { top_rgmii_txd[3] }]; #IO_L11P_T1_SRCC_13 Sch=eth_txd[3]

set_property -dict { PACKAGE_PIN AA14  IOSTANDARD LVCMOS25 } [get_ports { top_rgmii_txc }]; #IO_L5N_T0_13 Sch=eth_txck
set_property -dict { PACKAGE_PIN V10   IOSTANDARD LVCMOS25 } [get_ports { top_rgmii_tx_ctl }]; #IO_L10P_T1_13 Sch=eth_txctl

set_property -dict { PACKAGE_PIN V13   IOSTANDARD LVCMOS25 } [get_ports { top_rgmii_rxc }]; #IO_L13P_T2_MRCC_13 Sch=eth_rxck
set_property -dict { PACKAGE_PIN W10   IOSTANDARD LVCMOS25 } [get_ports { top_rgmii_rx_ctl }]; #IO_L10N_T1_13 Sch=eth_rxctl

set_property -dict { PACKAGE_PIN B18 IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports {top_io_2_lvds_clk_n}]
set_property -dict { PACKAGE_PIN B17 IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports {top_io_2_lvds_clk_p}]
set_property -dict { PACKAGE_PIN E18 IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports {top_io_2_lvds_valid_n}]
set_property -dict { PACKAGE_PIN F18 IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports {top_io_2_lvds_valid_p}]
set_property -dict { PACKAGE_PIN E17 IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports {top_io_2_lvds_frame_clk_n}]
set_property -dict { PACKAGE_PIN F16 IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports {top_io_2_lvds_frame_clk_p}]
set_property -dict { PACKAGE_PIN F20   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_2_lvds_data_n[0] }]; #IO_L18N_T2_16 Sch=fmc_la_n[20]
set_property -dict { PACKAGE_PIN F19   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_2_lvds_data_p[0] }]; #IO_L18P_T2_16 Sch=fmc_la_p[20]
set_property -dict { PACKAGE_PIN A19   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_2_lvds_data_n[1] }]; #IO_L17N_T2_16 Sch=fmc_la_n[19]
set_property -dict { PACKAGE_PIN A18   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_2_lvds_data_p[1] }]; #IO_L17P_T2_16 Sch=fmc_la_p[19]
set_property -dict { PACKAGE_PIN C17   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_2_lvds_data_n[2] }]; #IO_L12N_T1_MRCC_16 Sch=fmc_la18_cc_n
set_property -dict { PACKAGE_PIN D17   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_2_lvds_data_p[2] }]; #IO_L12P_T1_MRCC_16 Sch=fmc_la18_cc_p
set_property -dict { PACKAGE_PIN D21   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_2_lvds_data_n[3] }]; #IO_L23N_T3_16 Sch=fmc_la_n[22]
set_property -dict { PACKAGE_PIN E21   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_2_lvds_data_p[3] }]; #IO_L23P_T3_16 Sch=fmc_la_p[22]
set_property -dict { PACKAGE_PIN J17 IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports {top_io_3_lvds_clk_n}]
set_property -dict { PACKAGE_PIN K17 IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports {top_io_3_lvds_clk_p}]
set_property -dict { PACKAGE_PIN L13 IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports {top_io_3_lvds_valid_n}]
set_property -dict { PACKAGE_PIN M13 IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports {top_io_3_lvds_valid_p}]
set_property -dict { PACKAGE_PIN L18 IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports {top_io_3_lvds_frame_clk_n}]
set_property -dict { PACKAGE_PIN M18 IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports {top_io_3_lvds_frame_clk_p}]
set_property -dict { PACKAGE_PIN J21   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_3_lvds_data_n[0] }]; #IO_L11N_T1_SRCC_15 Sch=fmc_la01_cc_n
set_property -dict { PACKAGE_PIN J20   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_3_lvds_data_p[0] }]; #IO_L11P_T1_SRCC_15 Sch=fmc_la01_cc_p
set_property -dict { PACKAGE_PIN M22   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_3_lvds_data_n[1] }]; #IO_L15N_T2_DQS_ADV_B_15 Sch=fmc_la_n[06]
set_property -dict { PACKAGE_PIN N22   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_3_lvds_data_p[1] }]; #IO_L15P_T2_DQS_15 Sch=fmc_la_p[06]
set_property -dict { PACKAGE_PIN G20   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_3_lvds_data_n[2] }]; #IO_L8N_T1_AD10N_15 Sch=fmc_la_n[09]
set_property -dict { PACKAGE_PIN H20   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_3_lvds_data_p[2] }]; #IO_L8P_T1_AD10P_15 Sch=fmc_la_p[09]
set_property -dict { PACKAGE_PIN L20   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_3_lvds_data_n[3] }]; #IO_L14N_T2_SRCC_15 Sch=fmc_la_n[12]
set_property -dict { PACKAGE_PIN L19   IOSTANDARD LVDS_25 DIFF_TERM TRUE } [get_ports { top_io_3_lvds_data_p[3] }]; #IO_L14P_T2_SRCC_15 Sch=fmc_la_p[12]

#FMC SPI
set_property -dict { PACKAGE_PIN B15   IOSTANDARD LVCMOS25 } [get_ports { top_awr_host_intr1_fmc }]; #IO_L7P_T1_16 Sch=fmc_la_p[24]
set_property -dict { PACKAGE_PIN N20   IOSTANDARD LVCMOS25 } [get_ports { top_awr_host_intr2_fmc }]; #IO_L18P_T2_A24_15 Sch=fmc_la_p[04]
set_property -dict { PACKAGE_PIN N19   IOSTANDARD LVCMOS25 } [get_ports { top_awr_host_intr3_fmc }]; #IO_L17N_T2_A25_15 Sch=fmc_la_n[03]
set_property -dict { PACKAGE_PIN N18   IOSTANDARD LVCMOS25 } [get_ports { top_awr_host_intr4_fmc }]; #IO_L17P_T2_A26_15 Sch=fmc_la_p[03]
set_property -dict { PACKAGE_PIN D19   IOSTANDARD LVCMOS25 } [get_ports { top_awr_spi_cs1_fmc }]; #IO_L14N_T2_SRCC_16 Sch=fmc_la_n[21]
set_property -dict { PACKAGE_PIN E19   IOSTANDARD LVCMOS25 } [get_ports { top_awr_spi_cs2_fmc }]; #IO_L14P_T2_SRCC_16 Sch=fmc_la_p[21]
set_property -dict { PACKAGE_PIN M21   IOSTANDARD LVCMOS25 } [get_ports { top_awr_spi_cs3_fmc }]; #IO_L10P_T1_AD11P_15 Sch=fmc_la_p[05]
set_property -dict { PACKAGE_PIN B16   IOSTANDARD LVCMOS25 } [get_ports { top_awr_spi_cs4_fmc }]; #IO_L7N_T1_16 Sch=fmc_la_n[24]
set_property -dict { PACKAGE_PIN K21   IOSTANDARD LVCMOS25 } [get_ports { top_awr_spi_miso_fmc }]; #IO_L9P_T1_DQS_AD3P_15 Sch=fmc_la_p[10]
set_property -dict { PACKAGE_PIN M20   IOSTANDARD LVCMOS25 } [get_ports { top_awr_spi_mosi_fmc }]; #IO_L18N_T2_A23_15 Sch=fmc_la_n[04]
set_property -dict { PACKAGE_PIN K22   IOSTANDARD LVCMOS25 } [get_ports { top_awr_spi_clk_fmc }]; #IO_L9N_T1_DQS_AD3N_15 Sch=fmc_la_n[10]
set_property -dict { PACKAGE_PIN L21   IOSTANDARD LVCMOS25 } [get_ports { top_awr_nrst2_fmc }]; #IO_L10N_T1_AD11N_15 Sch=fmc_la_n[05]


#PMOD SPI
## Pmod header JA
set_property -dict { PACKAGE_PIN AB22  IOSTANDARD LVCMOS33 } [get_ports { top_awr_spi_cs1 }]; #IO_L10N_T1_D15_14 Sch=ja[1]
set_property -dict { PACKAGE_PIN AB21  IOSTANDARD LVCMOS33 } [get_ports { top_awr_spi_cs2 }]; #IO_L10P_T1_D14_14 Sch=ja[2]
set_property -dict { PACKAGE_PIN AB20  IOSTANDARD LVCMOS33 } [get_ports { top_awr_spi_miso }]; #IO_L15N_T2_DQS_DOUT_CSO_B_14 Sch=ja[3]
set_property -dict { PACKAGE_PIN AB18  IOSTANDARD LVCMOS33 } [get_ports { top_awr_spi_clk }]; #IO_L17N_T2_A13_D29_14 Sch=ja[4]
set_property -dict { PACKAGE_PIN Y21   IOSTANDARD LVCMOS33 } [get_ports { top_awr_spi_cs3 }]; #IO_L9P_T1_DQS_14 Sch=ja[7]
set_property -dict { PACKAGE_PIN AA21  IOSTANDARD LVCMOS33 } [get_ports { top_awr_spi_cs4 }]; #IO_L8N_T1_D12_14 Sch=ja[8]
set_property -dict { PACKAGE_PIN AA20  IOSTANDARD LVCMOS33 } [get_ports { top_awr_spi_mosi }]; #IO_L8P_T1_D11_14 Sch=ja[9]
#set_property -dict { PACKAGE_PIN AA18  IOSTANDARD LVCMOS33 } [get_ports { ja[7] }]; #IO_L17P_T2_A14_D30_14 Sch=ja[10]

## Pmod header JB
set_property -dict { PACKAGE_PIN V9    IOSTANDARD LVCMOS33 } [get_ports { top_awr_host_intr1 }]; #IO_L21P_T3_DQS_34 Sch=jb_p[1]
#set_property -dict { PACKAGE_PIN V8    IOSTANDARD LVCMOS33 } [get_ports { jb[1] }]; #IO_L21N_T3_DQS_34 Sch=jb_n[1]
set_property -dict { PACKAGE_PIN V7    IOSTANDARD LVCMOS33 } [get_ports { top_awr_host_intr2 }]; #IO_L19P_T3_34 Sch=jb_p[2]
#set_property -dict { PACKAGE_PIN W7    IOSTANDARD LVCMOS33 } [get_ports { jb[3] }]; #IO_L19N_T3_VREF_34 Sch=jb_n[2]
set_property -dict { PACKAGE_PIN W9    IOSTANDARD LVCMOS33 } [get_ports { top_awr_host_intr3 }]; #IO_L24P_T3_34 Sch=jb_p[3]
#set_property -dict { PACKAGE_PIN Y9    IOSTANDARD LVCMOS33 } [get_ports { jb[5] }]; #IO_L24N_T3_34 Sch=jb_n[3]
set_property -dict { PACKAGE_PIN Y8    IOSTANDARD LVCMOS33 } [get_ports { top_awr_host_intr4 }]; #IO_L23P_T3_34 Sch=jb_p[4]
#set_property -dict { PACKAGE_PIN Y7    IOSTANDARD LVCMOS33 } [get_ports { jb[7] }]; #IO_L23N_T3_34 Sch=jb_n[4]

## Pmod header JC
set_property -dict { PACKAGE_PIN Y6    IOSTANDARD LVCMOS33 } [get_ports { top_awr_nrst1 }]; #IO_L18P_T2_34 Sch=jc_p[1]
#set_property -dict { PACKAGE_PIN AA6   IOSTANDARD LVCMOS33 } [get_ports { jc[1] }]; #IO_L18N_T2_34 Sch=jc_n[1]
set_property -dict { PACKAGE_PIN AA8   IOSTANDARD LVCMOS33 } [get_ports { top_awr_nrst2 }]; #IO_L22P_T3_34 Sch=jc_p[2]
#set_property -dict { PACKAGE_PIN AB8   IOSTANDARD LVCMOS33 } [get_ports { jc[3] }]; #IO_L22N_T3_34 Sch=jc_n[2]
set_property -dict { PACKAGE_PIN R6    IOSTANDARD LVCMOS33 } [get_ports { top_awr_nrst1_pmod }]; #IO_L17P_T2_34 Sch=jc_p[3]
#set_property -dict { PACKAGE_PIN T6    IOSTANDARD LVCMOS33 } [get_ports { jc[5] }]; #IO_L17N_T2_34 Sch=jc_n[3]
#set_property -dict { PACKAGE_PIN AB7   IOSTANDARD LVCMOS33 } [get_ports { jc[6] }]; #IO_L20P_T3_34 Sch=jc_p[4]
#set_property -dict { PACKAGE_PIN AB6   IOSTANDARD LVCMOS33 } [get_ports { jc[7] }]; #IO_L20N_T3_34 Sch=jc_n[4]


set_property PACKAGE_PIN {T14} [get_ports {led_0}]
set_property IOSTANDARD {LVCMOS25} [get_ports {led_0}]
set_property PACKAGE_PIN {T15} [get_ports {led_1}]
set_property IOSTANDARD {LVCMOS25} [get_ports {led_1}]
set_property PACKAGE_PIN {T16} [get_ports {led_2}]
set_property IOSTANDARD {LVCMOS25} [get_ports {led_2}]
set_property PACKAGE_PIN {U16} [get_ports {led_3}]
set_property IOSTANDARD {LVCMOS25} [get_ports {led_3}]
set_property PACKAGE_PIN {V15} [get_ports {led_4}]
set_property IOSTANDARD {LVCMOS25} [get_ports {led_4}]
set_property PACKAGE_PIN {W16} [get_ports {led_5}]
set_property IOSTANDARD {LVCMOS25} [get_ports {led_5}]
set_property PACKAGE_PIN {W15} [get_ports {led_6}]
set_property IOSTANDARD {LVCMOS25} [get_ports {led_6}]
set_property PACKAGE_PIN {Y13} [get_ports {led_7}]
set_property IOSTANDARD {LVCMOS25} [get_ports {led_7}]
set_property BOARD_PIN {reset} [get_ports {reset}]
