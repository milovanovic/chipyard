import os, fileinput

old = "         led_7	// @[fpga/fpga-shells/src/main/scala/shell/IOShell.scala:150:18]"

filePath = "./generated-src/chipyard.fpga.nexysvideo.NexysVideoHarness.TinyRocketNexysVideoConfig/gen-collateral/NexysVideoHarness.sv"

lines = "         led_7, // @[fpga/fpga-shells/src/main/scala/shell/IOShell.scala:150:18]\n" +\
        "  input  awr_host_intr1_fmc,\n" +\
        "  input  awr_host_intr2_fmc,\n" +\
        "  input  awr_host_intr3_fmc,\n" +\
        "  input  awr_host_intr4_fmc,\n" +\
        "  output awr_spi_cs1_fmc,\n"    +\
        "  output awr_spi_cs2_fmc,\n"    +\
        "  output awr_spi_cs3_fmc,\n"    +\
        "  output awr_spi_cs4_fmc,\n"    +\
        "  input  awr_spi_miso_fmc,\n"   +\
        "  output awr_spi_mosi_fmc,\n"   +\
        "  output awr_spi_clk_fmc,\n"    +\
        "  output awr_nrst1_pmod,\n"     +\
        "  output awr_nrst2_fmc,\n"      +\
        "  \n"                           +\
        "  output awr_host_intr1,\n"     +\
        "  output awr_host_intr2,\n"     +\
        "  output awr_host_intr3,\n"     +\
        "  output awr_host_intr4,\n"     +\
        "  input  awr_spi_cs1,\n"        +\
        "  input  awr_spi_cs2,\n"        +\
        "  input  awr_spi_cs3,\n"        +\
        "  input  awr_spi_cs4,\n"        +\
        "  output awr_spi_miso,\n"       +\
        "  input  awr_spi_mosi,\n"       +\
        "  input  awr_spi_clk,\n"        +\
        "  input  awr_nrst1,\n"          +\
        "  input  awr_nrst2"

logic = "  reg intr1, intr2, intr3, intr4, cs1, cs2, cs3, cs4, nrst1, nrst2, spi_miso, spi_mosi, spi_clk;\n" +\
        "\n" +\
        "  always @(posedge _sys_clock_ibufg_O) begin\n" +\
        "    if(_WIRE_1) begin\n" +\
        "      intr1 <= 1'b0;\n" +\
        "      intr2 <= 1'b0;\n" +\
        "      intr3 <= 1'b0;\n" +\
        "      intr4 <= 1'b0;\n" +\
        "      cs1 <= 1'b0;\n" +\
        "      cs2 <= 1'b0;\n" +\
        "      cs3 <= 1'b0;\n" +\
        "      cs4 <= 1'b0;\n" +\
        "      nrst1 <= 1'b0;\n" +\
        "      nrst2 <= 1'b0;\n" +\
        "      spi_miso <= 1'b0;\n" +\
        "      spi_mosi <= 1'b0;\n" +\
        "      spi_clk <= 1'b0;\n" +\
        "    end\n" +\
        "    else begin\n" +\
        "      intr1 <= awr_host_intr1_fmc;\n" +\
        "      intr2 <= awr_host_intr2_fmc;\n" +\
        "      intr3 <= awr_host_intr3_fmc;\n" +\
        "      intr4 <= awr_host_intr4_fmc;\n" +\
        "      cs1 <= awr_spi_cs1;\n" +\
        "      cs2 <= awr_spi_cs2;\n" +\
        "      cs3 <= awr_spi_cs3;\n" +\
        "      cs4 <= awr_spi_cs4;\n" +\
        "      nrst1 <= awr_nrst1;\n" +\
        "      nrst2 <= awr_nrst2;\n" +\
        "      spi_miso <= awr_spi_miso_fmc;\n" +\
        "      spi_mosi <= awr_spi_mosi;\n" +\
        "      spi_clk <= awr_spi_clk;\n" +\
        "    end\n" +\
        "  end\n" +\
        "\n" +\
        "  assign awr_host_intr1 = intr1;\n" +\
        "  assign awr_host_intr2 = intr2;\n" +\
        "  assign awr_host_intr3 = intr3;\n" +\
        "  assign awr_host_intr4 = intr4;\n" +\
        "\n" +\
        "  assign awr_spi_cs1_fmc = cs1;\n" +\
        "  assign awr_spi_cs2_fmc = cs2;\n" +\
        "  assign awr_spi_cs3_fmc = cs3;\n" +\
        "  assign awr_spi_cs4_fmc = cs4;\n" +\
        "\n" +\
        "  assign awr_nrst1_pmod = nrst1;\n" +\
        "  assign awr_nrst2_fmc = nrst2;\n" +\
        "\n" +\
        "  assign awr_spi_miso = spi_miso;\n" +\
        "  assign awr_spi_mosi_fmc = spi_mosi;\n" +\
        "  assign awr_spi_clk_fmc = spi_clk;\n" +\
        "endmodule"

# Replace lines
for line in fileinput.FileInput(filePath, inplace=True):
    if old in line :
        line = lines + os.linesep
    if "endmodule" in line :
        line = logic + os.linesep
    print(line, end="")
    
    
#-----------------------------------------------------------------------------#
#-----------------------------------------------------------------------------#
#-----------------------------------------------------------------------------#
#-----------------------------------------------------------------------------#
xdc = "\n" +\
      "#FMC SPI\n" +\
      "set_property -dict { PACKAGE_PIN B15   IOSTANDARD LVCMOS25 } [get_ports { awr_host_intr1_fmc }]; #IO_L7P_T1_16 Sch=fmc_la_p[24]\n" +\
      "set_property -dict { PACKAGE_PIN N20   IOSTANDARD LVCMOS25 } [get_ports { awr_host_intr2_fmc }]; #IO_L18P_T2_A24_15 Sch=fmc_la_p[04]\n" +\
      "set_property -dict { PACKAGE_PIN N19   IOSTANDARD LVCMOS25 } [get_ports { awr_host_intr3_fmc }]; #IO_L17N_T2_A25_15 Sch=fmc_la_n[03]\n" +\
      "set_property -dict { PACKAGE_PIN N18   IOSTANDARD LVCMOS25 } [get_ports { awr_host_intr4_fmc }]; #IO_L17P_T2_A26_15 Sch=fmc_la_p[03]\n" +\
      "set_property -dict { PACKAGE_PIN D19   IOSTANDARD LVCMOS25 } [get_ports { awr_spi_cs1_fmc }]; #IO_L14N_T2_SRCC_16 Sch=fmc_la_n[21]\n" +\
      "set_property -dict { PACKAGE_PIN E19   IOSTANDARD LVCMOS25 } [get_ports { awr_spi_cs2_fmc }]; #IO_L14P_T2_SRCC_16 Sch=fmc_la_p[21]\n" +\
      "set_property -dict { PACKAGE_PIN M21   IOSTANDARD LVCMOS25 } [get_ports { awr_spi_cs3_fmc }]; #IO_L10P_T1_AD11P_15 Sch=fmc_la_p[05]\n" +\
      "set_property -dict { PACKAGE_PIN B16   IOSTANDARD LVCMOS25 } [get_ports { awr_spi_cs4_fmc }]; #IO_L7N_T1_16 Sch=fmc_la_n[24]\n" +\
      "set_property -dict { PACKAGE_PIN K21   IOSTANDARD LVCMOS25 } [get_ports { awr_spi_miso_fmc }]; #IO_L9P_T1_DQS_AD3P_15 Sch=fmc_la_p[10]\n" +\
      "set_property -dict { PACKAGE_PIN M20   IOSTANDARD LVCMOS25 } [get_ports { awr_spi_mosi_fmc }]; #IO_L18N_T2_A23_15 Sch=fmc_la_n[04]\n" +\
      "set_property -dict { PACKAGE_PIN K22   IOSTANDARD LVCMOS25 } [get_ports { awr_spi_clk_fmc }]; #IO_L9N_T1_DQS_AD3N_15 Sch=fmc_la_n[10]\n" +\
      "set_property -dict { PACKAGE_PIN L21   IOSTANDARD LVCMOS25 } [get_ports { awr_nrst2_fmc }]; #IO_L10N_T1_AD11N_15 Sch=fmc_la_n[05]\n" +\
      "\n" +\
      "#PMOD SPI\n" +\
      "## Pmod header JA\n" +\
      "set_property -dict { PACKAGE_PIN AB22  IOSTANDARD LVCMOS33 } [get_ports { awr_spi_cs1 }]; #IO_L10N_T1_D15_14 Sch=ja[1]\n" +\
      "set_property -dict { PACKAGE_PIN AB21  IOSTANDARD LVCMOS33 } [get_ports { awr_spi_cs2 }]; #IO_L10P_T1_D14_14 Sch=ja[2]\n" +\
      "set_property -dict { PACKAGE_PIN AB20  IOSTANDARD LVCMOS33 } [get_ports { awr_spi_miso }]; #IO_L15N_T2_DQS_DOUT_CSO_B_14 Sch=ja[3]\n" +\
      "set_property -dict { PACKAGE_PIN AB18  IOSTANDARD LVCMOS33 } [get_ports { awr_spi_clk }]; #IO_L17N_T2_A13_D29_14 Sch=ja[4]\n" +\
      "set_property -dict { PACKAGE_PIN Y21   IOSTANDARD LVCMOS33 } [get_ports { awr_spi_cs3 }]; #IO_L9P_T1_DQS_14 Sch=ja[7]\n" +\
      "set_property -dict { PACKAGE_PIN AA21  IOSTANDARD LVCMOS33 } [get_ports { awr_spi_cs4 }]; #IO_L8N_T1_D12_14 Sch=ja[8]\n" +\
      "set_property -dict { PACKAGE_PIN AA20  IOSTANDARD LVCMOS33 } [get_ports { awr_spi_mosi }]; #IO_L8P_T1_D11_14 Sch=ja[9]\n" +\
      "\n" +\
      "## Pmod header JB\n" +\
      "set_property -dict { PACKAGE_PIN V9    IOSTANDARD LVCMOS33 } [get_ports { awr_host_intr1 }]; #IO_L21P_T3_DQS_34 Sch=jb_p[1]\n" +\
      "set_property -dict { PACKAGE_PIN V7    IOSTANDARD LVCMOS33 } [get_ports { awr_host_intr2 }]; #IO_L19P_T3_34 Sch=jb_p[2]\n" +\
      "set_property -dict { PACKAGE_PIN W9    IOSTANDARD LVCMOS33 } [get_ports { awr_host_intr3 }]; #IO_L24P_T3_34 Sch=jb_p[3]\n" +\
      "set_property -dict { PACKAGE_PIN Y8    IOSTANDARD LVCMOS33 } [get_ports { awr_host_intr4 }]; #IO_L23P_T3_34 Sch=jb_p[4]\n" +\
      "\n" +\
      "## Pmod header JC\n" +\
      "set_property -dict { PACKAGE_PIN Y6    IOSTANDARD LVCMOS33 } [get_ports { awr_nrst1 }]; #IO_L18P_T2_34 Sch=jc_p[1]\n" +\
      "set_property -dict { PACKAGE_PIN AA8   IOSTANDARD LVCMOS33 } [get_ports { awr_nrst2 }]; #IO_L22P_T3_34 Sch=jc_p[2]\n" +\
      "set_property -dict { PACKAGE_PIN R6    IOSTANDARD LVCMOS33 } [get_ports { awr_nrst1_pmod }]; #IO_L17P_T2_34 Sch=jc_p[3]"

filePathXDC = "./generated-src/chipyard.fpga.nexysvideo.NexysVideoHarness.TinyRocketNexysVideoConfig/chipyard.fpga.nexysvideo.NexysVideoHarness.TinyRocketNexysVideoConfig.shell.xdc"
# apend lines
file1 = open(filePathXDC, "a")
file1.write(xdc)
file1.close()