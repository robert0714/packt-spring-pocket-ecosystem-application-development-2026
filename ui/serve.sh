#!/bin/bash
p=${1:-8020}
d=${2:-$PWD}
echo $d 
cd $d 
python -m http.server $p
# python  -m SimpleHTTPServer $p 
