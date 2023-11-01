#!/usr/bin/env sh

curl -sLO https://raw.githubusercontent.com/babashka/babashka/master/install > install
chmod +x install
sudo ./install
rm install
