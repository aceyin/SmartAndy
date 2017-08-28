# a node raspberry project

## requirements
1. snowboy - https://github.com/Kitt-AI/snowboy

  ```
  Precompiled node module
  
  Snowboy is available in the form of a native node module precompiled for: 64 bit Ubuntu, MacOS X, and the Raspberry Pi (Raspbian 8.0+). For quick installation run:
  
  npm install --save snowboy
  For sample usage see the examples/Node folder. You may have to install dependencies like fs, wav or node-record-lpcm16 depending on which script you use.
  ```

  ```
  Compile a Node addon
  
  Compiling a node addon for Linux and the Raspberry Pi requires the installation of the following dependencies:
  
  sudo apt-get install libmagic-dev libatlas-base-dev
  Then to compile the addon run the following from the root of the snowboy repository:
  
  npm install
  node-pre-gyp clean configure build
  ```
  
  
