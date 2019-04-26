import json
import sys
import zipfile
import os

types = {}

def getType(node):
    if node.has_key("t"):
        ts = node["t"]
        arr = ts.split("=")
        if len(arr) == 1:
            return types[arr[0]]
        elif len(arr) > 0:
            types[arr[0]] = ts
            return ts
    return ""

def getName(node):
    if node.has_key("n"):
        return node["n"]
    else:
        return ""

def cleanUpLocalChild(localChild):
    if  localChild.has_key("v") and isinstance(localChild["v"], list):
        children = localChild["v"]
        newChildren = []
        for child in children:
            if isinstance(child, dict) and getName(child) != "Remove":
                newChildren.append(cleanUpLocalChild(child))
        localChild["v"] = newChildren
    return localChild

def cleanUpLocalChilds(localChilds):
    newlocalChilds = []
    for localChild in localChilds:
        if getName(localChild) != "Create Local Device" and getName(localChild) != "Add Local Device by Connection String":
            newlocalChilds.append(cleanUpLocalChild(localChild))
    return newlocalChilds

def convertMain(childs):
    for child in childs:
        if getType(child).endswith("org.iot.dsa.iothub.IotHubNode"):
            for hubchild in child["v"]:
                if getName(hubchild) == "Local":
                    return cleanUpLocalChilds(hubchild["v"])



def convert(input_file_name, output_file_name):
    zf_in = zipfile.ZipFile(input_file_name, 'r')
    os.mkdir("tempfile_in")
    os.mkdir("tempfile_out")
    zf_in.extractall("tempfile_in")
    zf_in.close()
    with open("tempfile_in/nodes.json", "r") as inp:
        j = json.load(inp)
        rootchilds = j["v"]
        for rootchild in rootchilds:
            if getType(rootchild).endswith("org.iot.dsa.iothub.MainNode"):
                mainchilds = rootchild["v"]
                rootchild["v"]  = convertMain(mainchilds)

        out = open("tempfile_out/nodes.json", "w")
        json.dump(j, out, sort_keys=True, indent=4)
        out.close()
    zf_out = zipfile.ZipFile(output_file_name, mode='w')
    zf_out.write("tempfile_out/nodes.json", "nodes.json")
    zf_out.close()
    os.remove("tempfile_in/nodes.json")
    os.remove("tempfile_out/nodes.json")
    os.rmdir("tempfile_in")
    os.rmdir("tempfile_out")

args = sys.argv
inp_file = "in.zip"
out_file = "out.zip"
if len(args) > 1:
    inp_file = args[1]
if len(args) > 2:
    out_file = args[2]
convert(inp_file, out_file)