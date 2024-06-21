# Before installing
Don't forget to 

  * Check draft mails,
  * Check unpushed branches in repo
  * Browser bookmarks

Get a working device with the following connected:
  * This github repo accessed (and connected that you can modify it if needed)
  * A lastpass personal account connected. 

## Wipe out current os

- Shutdown the mac, 
- Press and hold Power button
- Go to options
- Select a known user on the laptop, enter its local password.
- Disk Utility, Select Macintosh HD - Data an erase, leave default options about AFPS and so on.
- Write the Apple ID, as the keyboard is not set, so @ is Shift 2 and . is :
- The password is the one stored in the icloud entry of lastpass.
- Switch to french
- Enter wifi password

TODO Turn 

# Setup the macos

## Macos setup
Connect bluetooth to keyboard + mouse + JBL

In Apple > System Settings
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
- Open [Chrome browser download page](https://www.google.com/chrome/)
   - Connect to chrome profile -> caumond@gmail.com
- Open [Arc browser download page](https://arc.net/)
   - Go to Password manager and unclick Offer to save passwords and Sign in automatically.
   - Create personal profile
      - Connect to Lastpass with personal account
      - Sign in
      - Install the plugin, sign in
      - Connect to Lastpass with personal account
   - Create sasu profile
      - Connect to Lastpass with anthony@caumond.fr account
      - Sign in
      - Install the plugin, sign in
      - Connect to Lastpass with personal account
   - Create hephaistox profile
right, then Install devices.
- Connect to WhatsApp
- Connect to Android Message
- Connect to Gitlab

# Start with drivers

# Prepare system configuration setup
- On the terminal:
  ```clojure
  mkdir Dev
  cd Dev
  git clone https://github.com/caumond/systematic_configuration.git
  cd systematic_configuration
  ```
- Do "bb init"
- Copy the public key to github > Profile > Settings >

# Install web apps
- Connect to [office](https://www.office.com/?auth=2) and click on "install app and more" on the top 
- Go to office 365 > Teams, install teams
- Install [OneDrive](https://www.microsoft.com/en-gb/microsoft-365/onedrive/download)
  - Connect with all accounts anthony@hephaistox.com, caumond@gmail.com
  - Open teams again, click on sync on all directories you'd like to synchronize
  - In anthonycaumond directory, select Hephaistos>Founders teams
  - Launch OneDrive app, Preferences > Files On-Demand > Donwload all OneDrive files now
- Install outlook
  - Connect to caumond@gmail.com
  - Connect to [anthony@caumond.com](https://help.ovhcloud.com/csm/fr-mx-plan-outlook-windows-configuration?id=kb_article_view&sysparm_article=KB0052099)
  - Which connection information will be
  ```
Entrant	ssl0.ovh.net	SSL/TLS	993
Sortant	ssl0.ovh.net	SSL/TLS	465
  ```
- Install [Notion app](https://www.notion.so/desktop)


# TODO
* bb ci-init is useless, as it is required before starting
* The bb ci-install should require no external deps, so malli should be moved to a corner.
* Install again.
* Check how folders / profiles could be kept in ARC

## Setup Microsoft apps
- Opening word, sign in the account, set in the docker
- Opening powerpoints, set in the docker
- Open outlook, 
   - Connect to caumond@gmail.com
- Go to `https://www.brother.fr/services-et-supports/drivers` and select DS-640 as a scanner to download the latest drivers
