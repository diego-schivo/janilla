# Janilla PetClinic Sample Application

## What's this?

This is a [Janilla](https://janilla.com/) version of the Spring PetClinic official sample application by [Spring](https://spring.io/).

The original application lives at <https://github.com/spring-projects/spring-petclinic>.

## Understanding the PetClinic application

A database-oriented application designed to display and manage information related to pets and veterinarians in a pet clinic.

## View a live demo

```browser
https://petclinic.janilla.com/
```

## Run PetClinic locally

Janilla PetClinic is a [Janilla](https://janilla.com/) application built using [Maven](https://maven.apache.org/). You can run it from Maven directly (it should work just as well with Java 25 or newer):

```bash
git clone https://github.com/diego-schivo/janilla-petclinic.git
cd janilla-petclinic
mvn compile exec:java
```

You can then access the PetClinic at <https://localhost:8443/>.

## In case you find a bug/suggested improvement for Janilla PetClinic

Our issue tracker is available [here](https://github.com/diego-schivo/janilla-petclinic/issues).

## Database configuration

In its default configuration, Janilla PetClinic stores its data in a file under the user home directory, which gets populated at startup with data.

You can change the file location by editing `configuration.properties` in the source package.

## Working with Janilla PetClinic in Eclipse IDE

[Step-by-step Video Tutorial](https://youtu.be/YdIv62obVsw) available on [Janilla YouTube Channel](https://www.youtube.com/@janilla).

### Prerequisites

The following items should be installed in your system:

- Java 25
- [Git command line tool](https://help.github.com/articles/set-up-git)
- Eclipse with the [m2e plugin](https://www.eclipse.org/m2e/)

In order to install them all:

1. Download the [Eclipse Installer](https://www.eclipse.org/downloads/packages/installer)
2. Install the package for Enterprise Java and Web Developers with JRE 25

### Steps

1. Launch Eclipse and choose Import projects from Git (with smart import)
2. Select GitHub as the repository source, then search for `janilla-petclinic fork:true` and complete the wizard
3. open the Java class `com.janilla.petclinic.PetClinicApplication` and launch Debug as Java Application
4. Open a browser and navigate to <https://localhost:8443/>

## Looking for something in particular?

| Item | Files |
| ---- | ----- |
| The Main Class| [PetClinicApplication](https://github.com/diego-schivo/janilla-petclinic/blob/main/source/com/janilla/petclinic/PetClinicApplication.java) |
| Configuration File| [configuration.properties](https://github.com/diego-schivo/janilla-petclinic/blob/main/source/com/janilla/petclinic/configuration.properties) |

## Contributing

The [issue tracker](https://github.com/diego-schivo/janilla-petclinic/issues) is the preferred channel for bug reports, feature requests and submitting pull requests.

## License

The Janilla PetClinic sample application is released under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
