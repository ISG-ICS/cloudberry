#!/usr/bin/env bash
fileid="0B423M7wGZj9dSDFNLWlwb29URmM"
destination="./script/sample.adm.gz"

# try to download the file
curl -c /tmp/cookie -L -o /tmp/probe.bin "https://drive.google.com/uc?export=download&id=${fileid}"
confirm=$(tr ';' '\n' </tmp/probe.bin | grep confirm)
confirm=${confirm:8:4}
curl -C - -b /tmp/cookie -L -o "$destination" "https://drive.google.com/uc?export=download&id=${fileid}&confirm=${confirm}"
