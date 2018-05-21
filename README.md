# Welcome to the bytemine manager

The *bytemine manager* is a graphical Desktop-Application
to manage OpenVPN Servers as well as users and certificates
involved in the process.

The *bytemine manager* requires a Java 1.6 or 1.7 (or to use Sun
versioning, 6 or 7) runtime environment.

## New bytemine manager repository

We had to start the bytemine manager repository from scratch. Sorry for the 
inconvenience.

## Building packages

### General requirements:

* JDK, at least version 6
* ant
* ant-optional package

__Windows__
In order to build the windows package simply call `ant` from your checkout directory. The archive will be created at `./dist/bytemine` and contains a `start-manager.jar` to run the software.

__Debian__
In order to build .deb packages on your machine at least the packages `build-essential`, `cdbs`, `debhelper` and `dpkg-dev` have to be installed, as the script uses `dpkg-buildpackage`.
If these packages are installed simply run `sh build-deb.sh` to create a .deb file inside the top directory of the current checkout directory.
You probably need to adjust the JAVA_HOME directive in debian/rules.

### Specials 

__Windows Executable__
To build a Windows executable file you have to install launch4j (http://launch4j.sourceforge.net/) on your building platform. You may need to adjust the path to your launch4j installation in build.xml by changing the value of property `launch4j.dir`.
The executable file will be contained inside the created archive.

__Manuals__
To create the manuals during build process a TeX system has to be installed on your building platform.

## Bundled dependencies

The *bytemine manager* comes with the following software components
bundled:

* bcprov-jdk16-145.jar: http://www.bouncycastle.org/java.html
* commons-io-1.4.jar: http://commons.apache.org/io/
* crypt4j-1.0.0.jar: http://www.ailis.de/~k/projects/crypt4j/
* jsch.jar: http://www.jcraft.com/jsch/
* yamlbeans-1.0.jar
* miglayout-3.6.3-swing.jar: http://www.miglayout.com/
* sqlitejdbc-v054.jar: http://www.zentus.com/sqlitejdbc/
* xstream-1.4.1.jar: http://xstream.codehaus.org/
* junit-4.7.jar: http://www.junit.org/
