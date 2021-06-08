# Abberior Files Fiji/ImageJ Plugin

Plugin to collate and streamline the import of Abberior .obf and .msr files into Fiji/ImageJ

## Getting Started

Open plugin, in Plugins -> EmoryICI -> Open Abberior Files

![image](https://user-images.githubusercontent.com/16306836/121113852-c3696c80-c7e0-11eb-8e1d-57308fae2e64.png)

Drag and drop as many .obf files as you like to the window.  Bioformats curently takes a little while parsing the very complete Abberior metadata, which is the reason for multithreaded UI and persistent files and thumbs; you should only need to wait once.

![image](https://user-images.githubusercontent.com/16306836/121113920-e005a480-c7e0-11eb-870f-e5c3e8b3e2d0.png)

Select files of interest.  Use select all and select none buttons if needed.

![image](https://user-images.githubusercontent.com/16306836/121114343-881b6d80-c7e1-11eb-8661-81fe8cde48ba.png)
![image](https://user-images.githubusercontent.com/16306836/121114401-9e292e00-c7e1-11eb-8d57-a53d0c828330.png)

Open highlighted/selected files in each file using open in Fiji orange arrow button:
![image](https://user-images.githubusercontent.com/16306836/121114531-d597da80-c7e1-11eb-9742-c8b38c8f58b4.png)


### Installing

Add `OpenAbberiorFiles-#.#.#-[SNAPSHOT].jar` to plugins folder of Fiji/ImageJ install.  See Releases for suggested versions to use until Fiji update site is setup.


