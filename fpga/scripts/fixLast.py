import os, fileinput

filePath = "./generated-src/chipyard.fpga.nexysvideo.NexysVideoHarness.TinyRocketNexysVideoConfig/gen-collateral/"
file1 = filePath + "SDFChainRadix2.sv"
file2 = filePath + "SDFChainRadix2_8.sv"
file3 = filePath + "SDFChainRadix2_16.sv"

# Replace lines
for line in fileinput.FileInput(file1, inplace=True):
    if line != None and "fireLast =" in line:
        line = "  wire fireLast = 0;" + os.linesep
    print(line, end="")
    
for line in fileinput.FileInput(file2, inplace=True):
    if line != None and "fireLast =" in line:
        line = "  wire fireLast = 0;" + os.linesep
    print(line, end="")
    
for line in fileinput.FileInput(file3, inplace=True):
    if line != None and "fireLast =" in line:
        line = "  wire fireLast = 0;" + os.linesep
    print(line, end="")
