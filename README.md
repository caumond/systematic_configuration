# systematic-configuration
The configuration is centrally managed here, so it is systematic / repeatable.

# Installation instructions

Install babashka's with 

``` clojure
bash < <(curl -s https://raw.githubusercontent.com/babashka/babashka/master/install)
```

# Design decisions

* Use babashka 
   * Rationale: 
      * It is a well known technology
   * Consequences
* Should enable many target platform
    * Rationale
      * For leverage both macos and linux hardwares we use
      * For a better separation of concern
    * Consequences 
      * configuration files are splitted between the goal and its setup from one side, and the how to from the other side
