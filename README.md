# Muser
**Muser is a procedural music generator.**  
It gives you a GUI to set parameters (or randomize them) to customize the song generation.  
A preview of the song can be heard by this GUI, and it can be exported on a MIDI File.  
The MIDI export is intended to be edited by other programs. Not to be used as is.  
If want to use a song from muser you can credit **Jarsick** as well as **Muser**.


## Installing
- The software is installable via Itch.io on [this link](https://jarsick.itch.io/muser) 
- Just unzip the downloaded file and click on the executable file you find here
- The Java version needs the user have Java 17(or higher) installed.

## Building
- Install a Java 17 (or newer) JDK.
- From PowerShell, run `.\build.ps1`.
- The runnable application will be created at `release\muser.jar`.
- Run it with `java -jar release\muser.jar`, or build and run it in one step with
  `.\build.ps1 run`.

Apache Ant is optional. If installed, `ant` produces the same release path.

On Linux, make the Bash script executable once and run the equivalent commands:

```bash
chmod +x build.sh
./build.sh                 # build release/muser.jar
./build.sh run             # build and launch Muser
./build.sh compile         # compile into build/classes
./build.sh clean           # remove generated files
```

## Reporting Issues
When you report an issue, please indicate:
- Muser version.
- Operating System.
- Processor
- Java version (if you are running the java build)
- Detailed steps needed to reproduce that issue
