# sonargraph-build-batch
Code to execute Sonargraph-Build for advanced analysis

This code demonstrates how Sonargraph-Build can be executed for a serious of different versions of a system.
The generated XML reports and snapshots are persisted to disk and uploaded to an instance of Sonargraph-Enterprise.

Of course, this can also be implemented as a batch script. But debugging and error handling is so much easier using a proper
programming language...

There are currently two analysis implemented:
1. Class com.hello2morrow.sonargraph.batch.analysis.CwaServerAnalysis demonstrates how tagged commits are retrieved from a 
Git repo, individual commits are checked out, built and analyzed with Sonargraph.
2. If building past releases is too complicated and time consuming the class com.hello2morrow.sonargraph.batch.analysis.HibernateCoreAnalysis demonstrates 
how the available releases are retrieved from a Maven repository, the jar and sources-jar are downloaded via a Maven build, 
retrieved from the local Maven repo and used in a Sonargraph software system which is then analyzed using Sonargraph-Build.

Configuration is done via properties files contained in src/main/resources.
Executing commands is currently only implemented for Windows (see com/hello2morrow/sonargraph/batch/shell/WindowsShell.java) 
but should not be complicated to be added for Unix-based operating systems.

If you want to run one of the existing analysis or create your own, you need a license for Sonargraph-Build.
An evaluation license can be requested at [https://www.hello2morrow.com/try_it](https://www.hello2morrow.com/try_it).

Further information about Sonargraph:
* [Homepage](https://www.hello2morrow.com/)
* [Sonargraph User Manual](https://eclipse.hello2morrow.com/doc/standalone/content/index.html)
* [Sonargraph-Build User Manual](http://eclipse.hello2morrow.com/doc/build/content/index.html)