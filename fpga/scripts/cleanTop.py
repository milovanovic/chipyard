import os
import sys
import fileinput


# tempFile = open("FFT2CFAR2RDandRAChainsWithGbemac.v", 'r+')

# for line in fileinput.input("FFT2CFAR2RDandRAChainsWithGbemac.v"):
#     if "module " in line:
#         module = line.replace("module ","")
#         module = module.replace("(","")
#         print(module)
# tempFile.close()

module = []
for line in fileinput.FileInput("FFT2CFAR2RDandRAChainsWithGbemac.v"):
    if line != None and "module " in line:
        tmp = line
        tmp = tmp.replace("module ","")
        tmp = tmp.split("(", 1)[0]
        if (tmp != "FFT2CFAR2RDandRAChainsWithGbemac"):
            module.append(tmp)

for mod in module:
    for line in fileinput.FileInput("FFT2CFAR2RDandRAChainsWithGbemac.v", inplace=True):
        if line != None and mod in line:
            line = line.replace(mod, "C_" + mod)
        # if line != None and "ioMem_" in line:
        #     line = line.replace("ioMem_", "auto_bus_in_")
        print(line, end="")