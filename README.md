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

## Some Sample Results
The analysis of several versions has been implemented to demonstrate how coupling and cyclic dependencies grow over time together
with the size of the projects. The following are a screenshots of Sonargraph-Enterprise which has been configured to show metrics for the system's size 
and coupling. 

The state of Hibernate-Core ([Hibernate-Core](https://github.com/hibernate/hibernate-orm/tree/main/hibernate-core)) is pretty severe:
* 89% of all lines of code are contained in files involved in cycle groups.
* The biggest component cycle group contains 2,211 out of 3,538 source files, the biggest package cycle group contains 
  264 of 283 packages.
* Average Component Dependency is ~2188, meaning the on average a source file depends on 2187 other source files.
  
![Trend of Hibernate-Core](/doc/Hibernate-Core_Entanglement.png "Trend of Hibernate-Core")
![Trend of Biggest Component Cycle in Hiberante-Core](/doc/Hibernate-Core_Biggest-Component-Cycle.png "Trend of Biggest Component Cycle in Hibernate-Core")


The state of CWA-Server ([CWA-Server](https://github.com/corona-warn-app/cwa-server)) is better, but also shows growing 
metrics for code involved in cycles.
* 39% of all lines of code are contained in files involved in cycle groups. 
* Cycle groups are not yet huge, but constantly growing.
![Trend of CWA-Server](/doc/CWA-Server_Entanglement.png "Trend of CWA-Server")
![Trend of Biggest Component Cycle in CWA-Server](/doc/CWA-Server_Biggest-Component-Cycle.png "Trend of Biggest Component Cycle in CWA-Server")
