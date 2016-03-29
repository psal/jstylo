JSAN - The Integrated JStylo and Anonymouth Package
====================================================

The Privacy, Security and Automation lab (PSAL)
Drexel University, Philadelphia, PA
http://psal.cs.drexel.edu/

----------------------------------------------------

JStylo
- Authorship recognition analysis tool
- Version: 1.2

----------------------------------------------------

License:

JStylo was released by the Privacy, Security and Automation lab at Drexel University in 2011 under the AGPLv3 license. It was ported to the BSD-3 clause license in 2013.
A copy of the current license is included with the repository/program. If for some reason it is absent, it can be viewed here: http://opensource.org/licenses/BSD-3-Clause

Dependencies:

JStylo now utilizes a maven POM file for managing dependencies. Note that there is one additional dependency (JGAAP) which is non-mavenized. 
This dependency jar is included with each release of JStylo. Include it in your project's lib folder to allow JStylo to access its resources.

Usage:

Jstylo requires Java 7 or later to run properly

In windows: double-click jstylo.jar
In other platforms / to view on-the-fly log:

> java [-Xmx2048m] -jar jsan.jar

Note:
For usage with large corpora or feature sets, it is recommended
to increase the JVM heap size using the -Xmx option.
 