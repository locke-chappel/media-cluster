Config
==
This is a simple tool for generating encrypted configuration files used by the Scheduler. This tool comes in 3 forms 1) the executable jar, 2) bundled POSIX shell script, and 3) native binaries (experimental).

This tool is not meant to be elegant but rather efficient and light weight. Are there better design practices? Sure, but at what cost? The goal is to be a light weight support tool that "just works".


#### Executable Jar
Just your standard executable jar. Run with `java -jar mediacluster-config-$version.jar`

**Requires JRE 17 or later**

#### POSIX Shell Script
The same jar file as mentioned above only pre-bundled in an easier to use shell script (basically the jar is just Base64 encoded and embedded into a self-extracting and self-executing script).

**Still Requires JRE 17 or later**

#### Native Binaries
*Experimental: You've been warned*

Native compiled binary using AOT to produce Linux amd64 (aka x86_x64) and Linux arm64v8 native binaries. These files are larger than the jar/script version; however, **no JRE is required**. That's right, these binaries can just run. The catch? Larger file size (however, the total size is smaller than the JRE + executable jar) and they are platform dependent, OS and arch (vs the the jar which will run on any Java enabled platform).

## How To Use
Select your desired version of the tool (including building from source if you really want to) and run it. The tool will prompt you for various values needed to run the Schedule instance. For most users the default values are correct. Some values (like passwords) do not have a default and you must provide them yourself.

Once complete the tool will generate two files, the key file and the encrypted config file. These files must be copied to your Scheduler instance (e.g. if using containers a mounted volume in the container at the proper location - see Scheduler's documentation for more details.)
