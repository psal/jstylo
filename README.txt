JSAN - The Integrated JStylo and Anonymouth Package
====================================================

The Privacy, Security and Automation lab (PSAL)
Drexel University, Philadelphia, PA
http://psal.cs.drexel.edu/

----------------------------------------------------

JStylo
- Authorship recognition analysis tool
- Version: 2.9.0

----------------------------------------------------

VERSION 2.9.0 NOTES:

Version 2.9.0 was produced by a limited-duration project specifically to update the JStylo API and backend infrastructure.
Updating and testing the UI was not a part of this project. If you intend to use the JStylo desktop UI, we recommend that you use branch 2.3.0.

License:

JStylo was released by the Privacy, Security and Automation lab at Drexel University in 2011 under the AGPLv3 license. It was ported to the BSD-3 clause license in 2013.
A copy of the current license is included with the repository/program. If for some reason it is absent, it can be viewed here: http://opensource.org/licenses/BSD-3-Clause

Dependencies:

JStylo now utilizes a Maven POM file for managing dependencies. Note that there is one additional dependency (JGAAP) which is non-mavenized. 
This dependency jar is included with each release of JStylo. Include it in your project's lib folder to allow JStylo to access its resources.

Building/Installing:

run a maven clean install either via command line or your IDE plugin. You can add the -DskipTests flag to improve the time on the build process.

Usage:

JStylo requires Java 8 or later to run properly

In windows: double-click jstylo.jar
In other platforms / to view on-the-fly log:

> java [-Xmx2048m] -jar jtylo.jar

To configure log4j, copy the log4j.xml file in src/main/resources and make the desired modifications.
Then, pass the VM the argument: -Dlog4j.configuration=file:///path/to/file/newlog4j.xml

Note:
For usage with large corpora or feature sets, it is recommended to increase the JVM heap size using the -Xmx option.

Logging:

JStylo uses Log4j for logging. To get log files for any experiments you run, add the VM argument -Dlog4j.configuration=file://${/path/to/log4j.xml}
A default log4j.xml file is included in src/main/resources
 