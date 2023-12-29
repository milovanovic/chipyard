`timescale 1ns / 1ps
`define PERIOD 5
`define LVDSPERIOD 3.333
`define RESETPOL 0
`define FFTSIZE 256
`define DATAWIDTH 16

module NexysVideoHarness_TB();
// LVDS data
reg [`DATAWIDTH-1:0] data_0_0 [`FFTSIZE-1:0];
reg [`DATAWIDTH-1:0] data_0_1 [`FFTSIZE-1:0];
reg [`DATAWIDTH-1:0] data_0_2 [`FFTSIZE-1:0];
reg [`DATAWIDTH-1:0] data_0_3 [`FFTSIZE-1:0];
reg [$clog2(`FFTSIZE)-1:0] k_0 = 0;
reg [$clog2(`DATAWIDTH)-1:0] m_0 = 0;
reg lvdssend_0 = 0;
reg [`DATAWIDTH-1:0] data_1_0 [`FFTSIZE-1:0];
reg [`DATAWIDTH-1:0] data_1_1 [`FFTSIZE-1:0];
reg [`DATAWIDTH-1:0] data_1_2 [`FFTSIZE-1:0];
reg [`DATAWIDTH-1:0] data_1_3 [`FFTSIZE-1:0];
reg [$clog2(`FFTSIZE)-1:0] k_1 = 0;
reg [$clog2(`DATAWIDTH)-1:0] m_1 = 0;
reg lvdssend_1 = 0;

reg sys_clock = 0;	
wire uart_txd;
wire uart_rxd;
// Chip 1
wire lvds_0_i_clk_p;
wire lvds_0_i_clk_n;
wire lvds_0_i_data_p_0;
wire lvds_0_i_data_p_1;
wire lvds_0_i_data_p_2;
wire lvds_0_i_data_p_3;
wire lvds_0_i_data_n_0;
wire lvds_0_i_data_n_1;
wire lvds_0_i_data_n_2;
wire lvds_0_i_data_n_3;
wire lvds_0_i_valid_p;
wire lvds_0_i_valid_n;
wire lvds_0_i_frame_p;
wire lvds_0_i_frame_n;
reg reg_lvds_0_i_clk_p = 1;
reg reg_lvds_0_i_clk_n = 0;
reg reg_lvds_0_i_data_p_0 = 0;
reg reg_lvds_0_i_data_p_1 = 0;
reg reg_lvds_0_i_data_p_2 = 0;
reg reg_lvds_0_i_data_p_3 = 0;
reg reg_lvds_0_i_data_n_0 = 1;
reg reg_lvds_0_i_data_n_1 = 1;
reg reg_lvds_0_i_data_n_2 = 1;
reg reg_lvds_0_i_data_n_3 = 1;
reg reg_lvds_0_i_valid_p = 0;
reg reg_lvds_0_i_valid_n = 1;
reg reg_lvds_0_i_frame_p = 0;
reg reg_lvds_0_i_frame_n = 1;
// Chip 2
wire lvds_1_i_clk_p;
wire lvds_1_i_clk_n;
wire lvds_1_i_data_p_0;
wire lvds_1_i_data_p_1;
wire lvds_1_i_data_p_2;
wire lvds_1_i_data_p_3;
wire lvds_1_i_data_n_0;
wire lvds_1_i_data_n_1;
wire lvds_1_i_data_n_2;
wire lvds_1_i_data_n_3;
wire lvds_1_i_valid_p;
wire lvds_1_i_valid_n;
wire lvds_1_i_frame_p;
wire lvds_1_i_frame_n;
reg reg_lvds_1_i_clk_p = 1;
reg reg_lvds_1_i_clk_n = 0;
reg reg_lvds_1_i_data_p_0 = 0;
reg reg_lvds_1_i_data_p_1 = 0;
reg reg_lvds_1_i_data_p_2 = 0;
reg reg_lvds_1_i_data_p_3 = 0;
reg reg_lvds_1_i_data_n_0 = 1;
reg reg_lvds_1_i_data_n_1 = 1;
reg reg_lvds_1_i_data_n_2 = 1;
reg reg_lvds_1_i_data_n_3 = 1;
reg reg_lvds_1_i_valid_p = 0;
reg reg_lvds_1_i_valid_n = 1;
reg reg_lvds_1_i_frame_p = 0;
reg reg_lvds_1_i_frame_n = 1;
// ETH
wire eth_phy_resetn;
wire eth_rgmii_txd_0;
wire eth_rgmii_txd_1;
wire eth_rgmii_txd_2;
wire eth_rgmii_txd_3;
wire eth_rgmii_tx_ctl;
wire eth_rgmii_txc;
wire eth_rgmii_rxd_0;
wire eth_rgmii_rxd_1;
wire eth_rgmii_rxd_2;
wire eth_rgmii_rxd_3;
wire eth_rgmii_rx_ctl;
wire eth_rgmii_rxc;
wire eth_mdc;
wire eth_mdio;
wire [15:0] ddr_ddr3_dq;
wire [1:0]  ddr_ddr3_dqs_n;
wire ddr_ddr3_dqs_p;
reg reset = `RESETPOL;
wire led_0;
wire led_1;
wire led_2;
wire led_3;
wire led_4;
wire led_5;
wire led_6;
wire led_7;

NexysVideoHarness DUT(
    .sys_clock(sys_clock),
    .uart_txd(uart_txd),
    .uart_rxd(uart_rxd),
    .top_io_2_lvds_clk_p(lvds_0_i_clk_p),
    .top_io_2_lvds_clk_n(lvds_0_i_clk_n),
    .top_io_2_lvds_data_p({lvds_0_i_data_p_3, lvds_0_i_data_p_2, lvds_0_i_data_p_1, lvds_0_i_data_p_0}),
    .top_io_2_lvds_data_n({lvds_0_i_data_n_3, lvds_0_i_data_n_2, lvds_0_i_data_n_1, lvds_0_i_data_n_0}),
    .top_io_2_lvds_valid_p(lvds_0_i_valid_p),
    .top_io_2_lvds_valid_n(lvds_0_i_valid_n),
    .top_io_2_lvds_frame_clk_p(lvds_0_i_frame_p),
    .top_io_2_lvds_frame_clk_n(lvds_0_i_frame_n),
    .top_io_3_lvds_clk_p(lvds_1_i_clk_p),
    .top_io_3_lvds_clk_n(lvds_1_i_clk_n),
    .top_io_3_lvds_data_p({lvds_1_i_data_p_3, lvds_1_i_data_p_2, lvds_1_i_data_p_1, lvds_1_i_data_p_0}),
    .top_io_3_lvds_data_n({lvds_1_i_data_n_3, lvds_1_i_data_n_2, lvds_1_i_data_n_1, lvds_1_i_data_n_0}),
    .top_io_3_lvds_valid_p(lvds_1_i_valid_p),
    .top_io_3_lvds_valid_n(lvds_1_i_valid_n),
    .top_io_3_lvds_frame_clk_p(lvds_1_i_frame_p),
    .top_io_3_lvds_frame_clk_n(lvds_1_i_frame_n),
    .top_phy_resetn(eth_phy_resetn),
    .top_rgmii_txd({eth_rgmii_txd_3, eth_rgmii_txd_2, eth_rgmii_txd_1, eth_rgmii_txd_0}),
    .top_rgmii_tx_ctl(eth_rgmii_tx_ctl),
    .top_rgmii_txc(eth_rgmii_txc),
    .top_rgmii_rxd({eth_rgmii_rxd_3, eth_rgmii_rxd_2, eth_rgmii_rxd_1, eth_rgmii_rxd_0}),
    .top_rgmii_rx_ctl(eth_rgmii_rx_ctl),
    .top_rgmii_rxc(eth_rgmii_rxc),
    .top_mdc(eth_mdc),
    .top_mdio(eth_mdio),
    .reset(reset),
    .led_0(led_0),
    .led_1(led_1),
    .led_2(led_2),
    .led_3(led_3),
    .led_4(led_4),
    .led_5(led_5),
    .led_6(led_6),
    .led_7(led_7),
    .top_awr_host_intr1_fmc(0),
    .top_awr_host_intr2_fmc(0),
    .top_awr_host_intr3_fmc(0),
    .top_awr_host_intr4_fmc(0),
    .top_awr_spi_cs1_fmc(),
    .top_awr_spi_cs2_fmc(),
    .top_awr_spi_cs3_fmc(),
    .top_awr_spi_cs4_fmc(),
    .top_awr_spi_miso_fmc(0),
    .top_awr_spi_mosi_fmc(),
    .top_awr_spi_clk_fmc(),
    .top_awr_nrst1_pmod(),
    .top_awr_nrst2_fmc(),
    .top_awr_host_intr1(),
    .top_awr_host_intr2(),
    .top_awr_host_intr3(),
    .top_awr_host_intr4(),
    .top_awr_spi_cs1(0),
    .top_awr_spi_cs2(0),
    .top_awr_spi_cs3(0),
    .top_awr_spi_cs4(0),
    .top_awr_spi_miso(),
    .top_awr_spi_mosi(0),
    .top_awr_spi_clk(0),
    .top_awr_nrst1(0),
    .top_awr_nrst2(0)
);

// Reset
initial begin
    #(50.1*`PERIOD) reset =~reset;
end;

// Initialize LVDS data
integer i,j;
reg [`DATAWIDTH/2-1:0] dataHigh_0_0, dataLow_0_0 = 0;
reg [`DATAWIDTH/2-1:0] dataHigh_0_1, dataLow_0_1 = 0;
reg [`DATAWIDTH/2-1:0] dataHigh_0_2, dataLow_0_2 = 0;
reg [`DATAWIDTH/2-1:0] dataHigh_0_3, dataLow_0_3 = 0;
reg [`DATAWIDTH/2-1:0] dataHigh_1_0, dataLow_1_0 = 0;
reg [`DATAWIDTH/2-1:0] dataHigh_1_1, dataLow_1_1 = 0;
reg [`DATAWIDTH/2-1:0] dataHigh_1_2, dataLow_1_2 = 0;
reg [`DATAWIDTH/2-1:0] dataHigh_1_3, dataLow_1_3 = 0;

initial begin
    for (i=0; i<`FFTSIZE; i = i + 1) begin
        dataLow_0_0 = i;
        dataLow_0_1 = i << 1;
        dataLow_0_2 = i << 2;
        dataLow_0_3 = i << 3;
        dataLow_1_0 = 8'h50;
        dataLow_1_1 = 8'h51;
        dataLow_1_2 = 8'h52;
        dataLow_1_3 = 8'h53;
        
        dataHigh_0_0 = 8'hA0;
        dataHigh_0_1 = 8'hA1;
        dataHigh_0_2 = 8'hA2;
        dataHigh_0_3 = 8'hA3;
        dataHigh_1_0 = i;
        dataHigh_1_1 = i << 1;
        dataHigh_1_2 = i << 2;
        dataHigh_1_3 = i << 3;
        
        data_0_0[i] = {dataHigh_0_0, dataLow_0_0};
        data_0_1[i] = {dataHigh_0_1, dataLow_0_1};
        data_0_2[i] = {dataHigh_0_2, dataLow_0_2};
        data_0_3[i] = {dataHigh_0_3, dataLow_0_3};
        data_1_0[i] = {dataHigh_1_0, dataLow_1_0};
        data_1_1[i] = {dataHigh_1_1, dataLow_1_1};
        data_1_2[i] = {dataHigh_1_2, dataLow_1_2};
        data_1_3[i] = {dataHigh_1_3, dataLow_1_3};
    end
end

// clock
always #`PERIOD sys_clock=~sys_clock;
// LVDS clock
always #(`LVDSPERIOD/2) begin 
    reg_lvds_0_i_clk_p=~reg_lvds_0_i_clk_p;
    reg_lvds_0_i_clk_n=~reg_lvds_0_i_clk_n;
    reg_lvds_1_i_clk_p=~reg_lvds_1_i_clk_p;
    reg_lvds_1_i_clk_n=~reg_lvds_1_i_clk_n;
end;
assign lvds_0_i_clk_p = reg_lvds_0_i_clk_p;
assign lvds_0_i_clk_n = reg_lvds_0_i_clk_n;
assign lvds_1_i_clk_p = reg_lvds_1_i_clk_p;
assign lvds_1_i_clk_n = reg_lvds_1_i_clk_n;

// LVDS data generation
always @(posedge reg_lvds_0_i_clk_p) begin
    if (k_0 == `FFTSIZE-1 & m_0 == `DATAWIDTH-1) begin
        lvdssend_0 = ~lvdssend_0;
    end;
end;

always @(posedge reg_lvds_0_i_clk_p or negedge reg_lvds_0_i_clk_p) begin
    if (k_0 < `FFTSIZE & m_0 == `DATAWIDTH-1) k_0 = k_0 + 1;
    m_0 = m_0 + 1;
end;

always @(posedge reg_lvds_0_i_clk_p or negedge reg_lvds_0_i_clk_p) begin
    if (lvdssend_0 == 1) begin
        reg_lvds_0_i_data_p_0 = data_0_0[k_0][m_0];
        reg_lvds_0_i_data_p_1 = data_0_1[k_0][m_0];
        reg_lvds_0_i_data_p_2 = data_0_2[k_0][m_0];
        reg_lvds_0_i_data_p_3 = data_0_3[k_0][m_0];
        reg_lvds_0_i_data_n_0 = ~reg_lvds_0_i_data_p_0;
        reg_lvds_0_i_data_n_1 = ~reg_lvds_0_i_data_p_1;
        reg_lvds_0_i_data_n_2 = ~reg_lvds_0_i_data_p_2;
        reg_lvds_0_i_data_n_3 = ~reg_lvds_0_i_data_p_3;
        reg_lvds_0_i_valid_p = 1;
        reg_lvds_0_i_valid_n = 0;
        if (m_0 < `DATAWIDTH/2) begin
            reg_lvds_0_i_frame_p = 1;
            reg_lvds_0_i_frame_n = 0;
        end
        else begin
            reg_lvds_0_i_frame_p = 0;
            reg_lvds_0_i_frame_n = 1;
        end;
    end
    else begin
        reg_lvds_0_i_data_p_0 = 0;
        reg_lvds_0_i_data_p_1 = 0;
        reg_lvds_0_i_data_p_2 = 0;
        reg_lvds_0_i_data_p_3 = 0;
        reg_lvds_0_i_valid_p = 0;
        reg_lvds_0_i_valid_n = 1;
        reg_lvds_0_i_frame_p = 0;
        reg_lvds_0_i_frame_n = 1;
    end;
end;

assign lvds_0_i_data_p_0 = reg_lvds_0_i_data_p_0;
assign lvds_0_i_data_p_1 = reg_lvds_0_i_data_p_1;
assign lvds_0_i_data_p_2 = reg_lvds_0_i_data_p_2;
assign lvds_0_i_data_p_3 = reg_lvds_0_i_data_p_3;
assign lvds_0_i_data_n_0 = reg_lvds_0_i_data_n_0;
assign lvds_0_i_data_n_1 = reg_lvds_0_i_data_n_1;
assign lvds_0_i_data_n_2 = reg_lvds_0_i_data_n_2;
assign lvds_0_i_data_n_3 = reg_lvds_0_i_data_n_3;
assign lvds_0_i_valid_p = reg_lvds_0_i_valid_p;
assign lvds_0_i_valid_n = reg_lvds_0_i_valid_n;
assign lvds_0_i_frame_p = reg_lvds_0_i_frame_p;
assign lvds_0_i_frame_n = reg_lvds_0_i_frame_n;


// LVDS data generation
always @(posedge reg_lvds_1_i_clk_p) begin
    if (k_1 == `FFTSIZE-1 & m_1 == `DATAWIDTH-1) begin
        lvdssend_1 = ~lvdssend_1;
    end;
end;

always @(posedge reg_lvds_1_i_clk_p or negedge reg_lvds_1_i_clk_p) begin
    if (k_1 < `FFTSIZE & m_1 == `DATAWIDTH-1) k_1 = k_1 + 1;
    m_1 = m_1 + 1;
end;

always @(posedge reg_lvds_1_i_clk_p or negedge reg_lvds_1_i_clk_p) begin
    if (lvdssend_1 == 1) begin
        reg_lvds_1_i_data_p_0 = data_1_0[k_1][m_1];
        reg_lvds_1_i_data_p_1 = data_1_1[k_1][m_1];
        reg_lvds_1_i_data_p_2 = data_1_2[k_1][m_1];
        reg_lvds_1_i_data_p_3 = data_1_3[k_1][m_1];
        reg_lvds_1_i_data_n_0 = ~reg_lvds_1_i_data_p_0;
        reg_lvds_1_i_data_n_1 = ~reg_lvds_1_i_data_p_1;
        reg_lvds_1_i_data_n_2 = ~reg_lvds_1_i_data_p_2;
        reg_lvds_1_i_data_n_3 = ~reg_lvds_1_i_data_p_3;
        reg_lvds_1_i_valid_p = 1;
        reg_lvds_1_i_valid_n = 0;
        if (m_1 < `DATAWIDTH/2) begin
            reg_lvds_1_i_frame_p = 1;
            reg_lvds_1_i_frame_n = 0;
        end
        else begin
            reg_lvds_1_i_frame_p = 0;
            reg_lvds_1_i_frame_n = 1;
        end;
    end
    else begin
        reg_lvds_1_i_data_p_0 = 0;
        reg_lvds_1_i_data_p_1 = 0;
        reg_lvds_1_i_data_p_2 = 0;
        reg_lvds_1_i_data_p_3 = 0;
        reg_lvds_1_i_valid_p = 0;
        reg_lvds_1_i_valid_n = 1;
        reg_lvds_1_i_frame_p = 0;
        reg_lvds_1_i_frame_n = 1;
    end;
end;

assign lvds_1_i_data_p_0 = reg_lvds_1_i_data_p_0;
assign lvds_1_i_data_p_1 = reg_lvds_1_i_data_p_1;
assign lvds_1_i_data_p_2 = reg_lvds_1_i_data_p_2;
assign lvds_1_i_data_p_3 = reg_lvds_1_i_data_p_3;
assign lvds_1_i_data_n_0 = reg_lvds_1_i_data_n_0;
assign lvds_1_i_data_n_1 = reg_lvds_1_i_data_n_1;
assign lvds_1_i_data_n_2 = reg_lvds_1_i_data_n_2;
assign lvds_1_i_data_n_3 = reg_lvds_1_i_data_n_3;
assign lvds_1_i_valid_p = reg_lvds_1_i_valid_p;
assign lvds_1_i_valid_n = reg_lvds_1_i_valid_n;
assign lvds_1_i_frame_p = reg_lvds_1_i_frame_p;
assign lvds_1_i_frame_n = reg_lvds_1_i_frame_n;

endmodule
