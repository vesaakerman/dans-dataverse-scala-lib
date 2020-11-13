MANUAL
======
[![Build Status](https://travis-ci.org/DANS-KNAW/dans-dataverse-scala-lib.png?branch=master)](https://travis-ci.org/DANS-KNAW/dans-dataverse-scala-lib)

Library with classes and functions for interacting with the Dataverse API.

DESCRIPTION
-----------
Dataverse is an open source web application to share, preserve, cite, explore, and analyze research data. 
See: <https://dataverse.org/about>. It has several APIs that enable programmatic access. 
See: <https://guides.dataverse.org/en/latest/api/index.html>. This library facilitates accessing this 
API from Scala and Java code.

INSTALLATION
------------

To use this library in a Maven-based project:

1. Include in your `pom.xml` a declaration for the DANS maven repository:

        <repositories>
            <!-- possibly other repository declarations here ... -->
            <repository>
                <id>DANS</id>
                <releases>
                    <enabled>true</enabled>
                </releases>
                <url>https://maven.dans.knaw.nl/releases/</url>
            </repository>
        </repositories>

2. Include a dependency on this library. The version should of course be
   set to the latest version (or left out, if it is managed by an ancestor `pom.xml`).

        <dependency>
            <groupId>nl.knaw.dans.lib</groupId>
            <artifactId>dans-dataverse-scala-lib_2.12</artifactId>
            <version>1.0.0</version>
        </dependency>
