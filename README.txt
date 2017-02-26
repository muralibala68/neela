
Neela : Peer-to-peer distributed and concurrent file sharing system

Overview:
This is a simple command line based file sharing application i.e., there is no GUI frontend.

What are the technologies used?
This is based on the following open source technologies:
1. All peer-to-peer comunication is over Google gRPC
2. Message payload is using Google Protobuf v3
3. Application framework is based on Spring
4. Maven 3.3.9
5. Developed on Eclipse Neon IDE
6. JDK 1.8.0 update 121

How to deploy?
Simply unzip the attached to a convenient location

What are the pre-requisites to build and run?
Please ensure Maven 3.3.9 and JDK 1.8.0 are on the path.

How to build?
It can be built either from Eclipse IDE or from command prompt.
To build from Windows command prompt or bash:
. cd to .../neela directory, and run the following:
. mvn clean install
This should create a jar with all dependencies included in .../neela/target directory

How to run?
It can be run from Eclipse IDE or from command prompt.
To run from Windows command prompt or bash:
cd to .../neela directory, and run the following:
java -cp target/neela-1.0-SNAPSHOT-jar-with-dependencies.jar org.bala.neela.fs.Neela

What are the user commands supported by this system?
type 'help' to see a list of commands supported
HELP
LISTPEERS
BROWSE   <hostname>
SEARCH   <hostname>
DOWNLOAD <filename>
UPLOAD   <filename>:<toHostname>
QUIT

What happens if the file being downloaded or uploaded already exists?
Filename is appended with timestamp e.g. sample.txt.1487865345519

What type of files that are supported?
Currently, it supports only ASCII text files. However, it can be extended to support binary files too.

Is there a log file to view?
Yes. .../neela/log/neela.log

Does it require a BootStrapping peer?
Yes. would help.

Is there a PeerRegister and where is it located?
Yes. .../neela/share/PeerRegister.txt

Where does it keep any downloaded files?
.../neela/share

Where does it keep any shareable files? i.e., for remote peers to download
Same location as where it keeps the downloaded files i.e. .../neela/share

How is the PeerRegister distributed across peers?
Every Peer when it comes up connects to a known peer to bootstrap i.e., to get the PeerRegister.
Apart from this, any peer when trying to browse a remote peer gives its register to it.

Where is the bootstrapping peer address is kept?
It is in the spring config and requires a rebuild.

How concurrent this application is?
Each user command is processed asynchronously. Hence, the user can continue to submit commands
without waiting for any previous commands to complete.

How many peers could be connected?
There is no implicit limit imposed by the application; may be limited by the hardware resources.

What is the port number used?
Listens on port 51162 for incoming requests from remote peers.

Is this port changeable?
Yes. Please refer to the Spring configuration found in ServiceConfig.java for changing it.
Changing the port number requires a rebuild.

Where all configurable parameters found?
All in the Spring config class: neela/src/main/java/org/bala/neela/wiring/GrpcServiceConfig.java
Actually, there is no need to reconfigure anything to run the application.

Cheers
Bala, 22nd-Feb-2017