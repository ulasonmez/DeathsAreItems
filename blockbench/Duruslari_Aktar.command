#!/bin/zsh
cd -- "${0:A:h}/.." || exit 1
python3 tools/blockbench/display_projects.py apply --write
result=$?
print '\nKapatmak için Enter tuşuna basın.'
read -r
exit $result
