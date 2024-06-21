#!/usr/bin/env sh

echo "Installing homebrew"
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
(echo; echo 'eval "$(/opt/homebrew/bin/brew shellenv)"') >> /Users/anthonycaumond/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv)"

echo "Openjdk installation"
brew install openjdk
echo 'export PATH="/opt/homebrew/opt/openjdk/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

echo "Babashka installation"
brew install borkdude/brew/babashka

echo "You can start with `bb init` now."
