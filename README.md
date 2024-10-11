Media Cluster
==
This project is a media scheduler service that can encode a video file using multiple networked nodes. Unlike many other projects of this nature, this project is encodes a single video file using multiple nodes by splitting the encoding process into segments, handing them out to the worker nodes, and then reassembling the pieces back into a single file. Multiple parallel encoding workflows are supported by creating multiple worker clusters (a video file is assigned to a single cluster for all processing jobs).

# THIS PROJECT IS ALPHA STAGE
While the main idea and core functionality is present, the project is not yet feature complete. Likewise testing is not complete and I expect there are bugs still. Feel free to file issues/feature requests but also be aware that this is a hobby project - I make no assurances of timelines. I will at least try to acknowledge and comment on issues I see logged but implementing them is a different matter entirely.

**_Use at your own risk_**

## Assumptions/Requirements
Each node (i.e. the scheduler and each worker node) requires access to the source video file and a common location to store the intermediate files. Typically this implies a network share that each node has read and write permissions to (SMB shares are known to work here, NFS should work too as should any other shared file system provided the permissions are correct).

Since this is a video encoding project there will be lots of CPU and to a lesser extent I/O resources needed - the goal here is consume as much CPU as is reasonable per node to encode each segment as fast as possible. To speed up overall encoding of a file just add more nodes to the cluster.

Note that significantly mismatched nodes could affect overall encode time, i.e. given a single fast node and a single slow node (speed is relative in this example) it's possible for the fast node to complete multiple jobs during the same wall clock time that the slow node completes just a single jobs. In the extreme cases this can mean that the fast node does almost all the work and ends up having to wait until the slow node completes its single task which can ultimately be slower than just using the single fast node. You are most likely to encounter this when mixing fast, current generation x64 CPU based nodes with ARM based single board computer nodes (especially if they are older/slower ARM nodes) - the bigger the performance discrepancy the larger the potential impact is. _This does not affect the final encode quality, only the clock time to complete (assuming you aren't using CPU/GPU specific features that are not available on all nodes)._

Concerning GPU acceleration - this mode is not tested and may not work as expected. The project's goal is to perform distributed CPU encoding. That being said, if all GPUs in a cluster are identical it should be possible. Mixing GPUs with different hardware encoding capabilities may result in different sections of the final video looking a little different due to different encoding options. You may also have to fine tune the slice length to ensure the number of frames each job process results in a compatible segment for the hardware encoder to correctly process (e.g. you might need to ensure each segment doesn't contain "padding" to reach some unit size). All of this considered, hardware based encoding is not officially supported but it is, at least in theory, plausible if you are careful when selecting your cluster's hardware.

Lots of storage and CPU will be needed, a decent amount of memory too. The process will consume as much CPU as you allocate it and storage should require at most 4x a single file's space* (presumably the encoding will result in files no larger than the original source though, most often much smaller as that's the main purpose here). Exact resources obviously very greatly depending on source files and selected encoding settings (it's technically possible for the final encoding to be larger than the source).

Mutli-stream selection is not directly supported. Meaning if you have multiple video streams, multiple audio streams, etc. They all are process with the default mappings. You can manually tune the encoder settings in each profile if desired; however, the general model is based on the assumption of 1 video stream and 1 audio stream per source. Also note that subtitles are not directly supported either.

* The actual storage needed is technically 1x the source file plus 1x the final encoded size as slices, plus 1x the final encoded size as a merged file, plus 1x the final audio file, plus 1x the final audio/video muxed file. Upon successful processing all files except for the final muxed file are deleted, **including the original source**.

## Initial Configuration
While the application tries make reasonable guesses about it's environment, you are strongly encouraged to review the [Scheduler Node's](#scheduler-node) settings prior to configuring any [Worker Nodes](#worker-node). Changing the Scheduler's settings can impact what configuration a Worker requires so ensuing the Scheduler is properly configured prior to adding Workers can save some rework.

Of primary concern is the Scheduler's URL (Menu -> Settings in the Scheduler Settings section). Make sure this value is reachable by all Workers, including ensuring any TLS settings and proxies are properly configured (i.e. ensure that every worker can access the Scheduler via this URL). If you are using any private TLS certificates please ensure proper configuration of the Worker nodes as well.

## General Design
### Scheduler Node
This node is responsible coordinating the processing of all files. This node will not perform any processing but it will have to move files around between the intake location and all cluster locations. This node is also responsible for handing out each unique job to the workers as well in what sequence. This node is a durable node and will survive restarts unaffected. _This node requires persistent storage in addition to read/write access to the video file location._ **There is only one scheduler node.**

### Worker Node
Worker nodes are responsible for handling the actual scanning, encoding, merging, and muxing steps. These nodes do not require any special configuration or storage beyond access to it's cluster's source files and output locations. Each worker node will process only one job at a time but any worker node may process any job type that is has been configured for (this is configured via the Scheduler Node). You must have at least one worker node in order for files to be processed; however, you have as many additional workers as you desire - there is no maximum limit. A general rule of thumb is to limit each physical host to a single worker node as the process is very CPU intensive but that being said there are always interesting cases where specific platforms perform better with multiple nodes - just remember that each node assumes it gets to consume all the resources it can see. You may also limit a worker to at least one job type; however, a cluster must have at least one worker able to process every job type. This can be a single node or multiple node so long at least one worker can process each job type (this can be useful if you want designated scan nodes or to prevent video encoding on certain slower nodes).

### Jobs
When a file is first submitted to the Scheduler Node it will be scanned and a number of jobs will be created for that source file. There are five kinds of jobs: 1) scan job, 2) video encoding job, 3) audio encoding job, 4) merge job, and 5) muxing job.

1. The scan job determines how many additional jobs are required based on the number of frames in the source as well if audio is included
2. The video encoding job is responsible for encoding a segment of the source video (default is 1,000 frames) into a temporary video-only file
3. The audio encoding job is responsible for encoding the _entire_ source file's audio stream into the target format in an audio-only file. Audio encoding is "fast-enough" that splitting it across multiple nodes results in more errors than benefits.
4. The merge job is responsible for sequentially combining each individual temporary video-only file into a single temporary video stream file (this is an independent step due to how ffmpeg handles concatenation streams). This job is largely I/O bound as it does not perform any encoding steps, it simply recombines the various parts back into a single file.
5. The muxing job is responsible for copying merged video-only file and the audio-only file into the final output container. This job is largely I/O bound as it does not perform any encoding steps, it simply recombines the audio and video parts back into a single file.

## Native Binaries
Some components may have native Linux amd64 and arm64v8 binaries. These are produced using Ahead of Time compilation of the Java programs. This is very new technology so they are considered experimental. That being said, they typically run faster and with less resources than their Java originals. Most of the resource consumption will be due to ffmpeg so this optimization may not be as useful as it seems. Still on slow hardware the difference could help. If you run into issues with these versions feel free to log a bug but please explicitly state that you using the native binary (including which exact version and arch). Bugs in native binaries may get resolved or they may be impossible to resolve at this time. These are provided as-is as a possibly useful but **highly experimental** feature - _they are not the primary use case_.

### Compiling Native Binaries from Source
As this is open source you may compile native versions yourself (feel free to submit PRs for any fixes you discover along the way). Note that native compilation is _very_ memory intensive compared to typical Java builds - you will need 8GB+ of RAM available to the builder process and it can take a long time (and hour or more per compile on slow CPUs). I recommend testing any builds on a relatively modern x64 system with lots of memory before attempting this on an ARM based Single Board Computer. Faster x64 system can usually compile this project in a "reasonable" 10-20 minutes or so per app (so yes a full ARM build on something like a Raspberry Pi 4 8GB will take around 3 hours to complete as each project will have be compile one at a time in order to fit into memory and the CPU will be fully loaded). Also note that there is no ARM-6 or ARM-7 support so no ability to make a native image for earlier generation SBCs, this is an AOT compiler limitation. Native Windows builds are theoretically possible too, however, I do not have a build environment for this (it's a bit more involved to setup than native Linux builds).

## Setup Instructions
This is the basic setup flow for creating a scheduler and one or more workers. The exact details depends on how you are deploying the instances (Docker CLI, docker-compose, Kubernetes, etc.)

### Scheduler Setup (once)
1. Use the `config` tool and follow its prompts to generate an encrypted config file and key. Make note of where you saved the key and config files to.
2. Create an app-data directory for the Scheduler instance's settings. Copy the key and config files from step 1 to there. Additionally, you may also create an application.properties file with any customizations desired (typical Java/Spring/Spring Boot plus some application specific options are available, see the [Scheduler's application.properties](scheduler/src/main/resources/application.properties) file for some examples).
3. For the first run of the Scheduler you will need to ensure two environment variables are set: `application.firstrun.username` and `application.firstrun.password`. These will be used only on the first run to establish the initial user account. You should use a temporary value here as the next step will change it. 
4. Start the scheduler instance.
5. Log into the scheduler and change your password on the Settings page (this will ensure any clear text copies of your initial password are no longer valid). At this point you should remove the two environment variables from your scheduler deployment - they will not be used again.
6. Update any scheduler settings (in particular the URL) - all worker nodes will need to be able reach this URL.


### Worker Setup (for each worker)
7. In the scheduler UI, create a worker instance and copy the configuration value.
7. Create an environment variable in the worker instance with the name `WORKER_CONFIG` and set it's value to the configuration string you copied in the previous step. Additionally, you may also create an application.properties file with any customizations desired (typical Java/Spring/Spring Boot plus some application specific options are available, see the [Worker's application.properties](worker/src/main/resources/application.properties) file for some examples).
9. Start the worker instance.


#### General Notes
* When specifying paths in an `application.properties` file, escape spaces in the path with '\\ ' - the first backslash escapes the backslash in the properties file so that at runtime the actual string is just backslash + space. Unescaped spaces are interpreted as arguments to the command. (Of course simply avoiding the use of spaces in file paths is the easiest option.)
* By default the scheduler runs on TCP port 8080 while the worker run on TCP port 8081. This was done to allow the same node to run both a scheduler and a worker. While you may not want more than one worker per node, the scheduler likely doesn't require dedicated resources and will happily share space with a single worker in most cases.

## General Security Advisory
At it's core this application suite is in effect a remote command executor. It has, however, been designed to minimize the risks of such a tool. For example, by default the application invokes the ffmpeg binary directly and not via a shell so typical shell exploits likely won't work. However, ffmpeg itself does take arbitrary files as input so it's plausible that a well crafted job request could attempt to extract data from the file system (pending file system permissions of course) and/or potential exploits in/via ffmpeg.

It's strongly recommended to run this application suite in a container to reduce these risks as much as possible - if a malicious user can only access the container's contents then potential damage is limited even further. To that end, this application suite is designed with Docker containers as the preferred deployment design and is intended to be compatible with the usual container hardening measures (e.g. it runs as a non-root user, the container itself can be read-only, all security options can be dropped and forbidden from being re-added, etc.).

Generally speaking, it's advisable to run TLS on all connections (e.g. HTTPS instead of HTTP); however, configuring internal TLS can be difficult especially for home labs. So while TLS connections are supported all communication between the scheduler and worker nodes uses signed message payloads with time stamps to prove authenticity and to limit replay attacks. The scheduler and workers never exchange sensitive information and each is able to verify the request came from the specific node that signed it (each worker has a unique signature). The only area left exposed that may have sensitive values are the login page and the settings page when passwords are entered. For these reasons the application does not default to using TLS. You may implement TLS connections using the standard Spring Boot and Java configurations (i.e. enable TLS support in Spring Boot and add any required keys and certificates the JVM's trust and key stores). You may also use reverse proxy and/or TLS termination patterns (commonly found in Kubernetes and similar designs) to handle the TLS work for you. Just remember that the scheduler and worker node will initiate HTTPS requests to the URLs configured and if those URLs use TLS the JVM will need to be able to trust the certificate at each end point. One other reason to run TLS is simply convenience, web browsers are more and more starting to complain and add extra hoops to jump through just to connect to a non-TLS HTTP server. Configuring TLS avoids this (at the cost of learning how to set up TLS though). You could also run in mixed mode - technically running the scheduler in both HTTP and HTTPS in parallel would allow human interaction over TLS (thus protecting your password and silencing the browser complaints) while still easing the scheduler <-> worker interactions by allowing them to use clear text HTTP; however, this may ultimately be harder to manage than simply running all TLS.

Lastly, this was design for use in a home lab type environment. As such it was never intended to run over public or otherwise untrusted networks. Sure, standard web security principles are implemented but ultimately this application suite was never meant for use over non-LAN networks. Also remember that all nodes will need to access the source files so this design model is rather network bandwidth intensive too (it may not need a _fast_ network connection but it _will_ transfer a lot of data over the connection so probably not wise to use a metered connection where you pay by the GB). Also many network file system protocols are also not intended for use on WAN connections either and likely also have their own security considerations. Could you use a VPN? Sure, but overall this is not a supported use case.

Ultimately this application suite was designed to be "secure enough" for it's intended use case. Extra layers of security may be applied if desired but likely aren't necessary for the target audience.