# Setup the macos

## Macos setup
Connect bluetooth to keyboard + mouse + JBL
Setup
-> Trackpad -> Scroll & Zoom > Natural Scrolling
-> Trackpad -> Secondary click > Click in Bottom right corner

Finder setup
- Unselect items on dekstop
- New finder windows show as "anthonycaumond", the home
- Tab Tags, remove all colors from the sidebar
- Sidebar, select only: Applications, Desktop, Docuemnts, Downloads, anthonycaumond, Pictures, Connected servers
- Advanced tab: Keep foldersr on top in windows when sorting by name
- Search the Current Folder when performing a search

# Start with www
- Open safari browser
- Open Chrome
   - Setup Chrome > settings > default browser > make default
   - Connect to chrome profile -> compte caumond sur Tel.
- Connect to WhatsApp
- Connect to Android Message
- Connect to Lastpass
- Connect to Gitlab
- Connect to GitHub

# Start with drivers
## Epson 4100
See [Epson website](https://www.epson.fr/fr_FR/support/sc/epson-expression-home-xp-4100/s/s1729)
 to install it

# Install web apps
- Download Firefox, https://www.mozilla.org/fr/firefox/download/thanks/
- Download 365
- Install office
- Go to office 365 > Teams, install teams
- Install [OneDrive](https://www.microsoft.com/en-gb/microsoft-365/onedrive/download)
  - Connect with all accounts anthony@hephaistox.com, caumond@gmail.com
  - Open teams again, click on sync on all directories you'd like to synchronize
  - In anthonycaumond directory, select Hephaistos>Founders teams
  - Launch OneDrive app, Preferences > Files On-Demand > Donwload all OneDrive files now
- Install iterm2
- Install outlook
  - Connect to caumond@gmail.col
  - Connect to [anthony@caumond.com](https://help.ovhcloud.com/csm/fr-mx-plan-outlook-windows-configuration?id=kb_article_view&sysparm_article=KB0052099)
  - Which connection information will be
  ```
Entrant	ssl0.ovh.net	SSL/TLS	993
Sortant	ssl0.ovh.net	SSL/TLS	465
  ```
- Install https://www.notion.so/desktop

## Setup Microsoft apps
- Opening word, sign in the account, set in the docker
- Opening powerpoints, set in the docker
- Open outlook, 
   - Connect to caumond@gmail.com
- Go to `https://www.brother.fr/services-et-supports/drivers` and select DS-640 as a scanner to download the latest drivers

# Install bash like apps
- Open iTerm2
- Setup iTerm2:
   - General > Closing > Quit when all windows are closed
   - Profiles > Default > General > Working Directory > Reuse previous section's directory
- Install brew with
```shell
git config --global user.email "anthony@caumond.com"
git config --global user.name "Anthony CAUMOND"

npm install -g typewritten
yes | sh -c "$(curl -fsSL https://raw.github.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"

npm -g install js-beautify
npm install --save-dev -g stylelint stylelint-config-standard
npm install -g node-cljfmt

mkdir -p ~/Dev/install
cd ~/Dev/install
wget http://ftp.gnu.org/gnu/glpk/glpk-5.0.tar.gz 
tar -xvzf glpk-5.0.tar.gz
cd glpk-5.0
./configure

git clone --depth 1 https://github.com/doomemacs/doomemacs ~/.config/emacs
yes | ~/.config/emacs/bin/doom install

sudo mkdir -p /usr/local/bin
curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > lein
chmod +x lein
sudo mv lein /usr/local/bin/

# Installing
ssh-keygen -t rsa -b 4096 -C "anthony@caumond.com"
cat ~/.ssh/id_rsa.pub
# Now copy that key in 
```
