# Janilla Blank Template

This is a porting of [Payload Blank Template](https://github.com/payloadcms/payload/tree/main/templates/blank).

### View a live demo

Open a browser and navigate to <https://blanktemplate.janilla.com/>.

### How you can get started

> **_Note:_**  if you are unfamiliar with the terminal, you can set up the project in an IDE (section below).

Make sure you have Java SE Platform (JDK 25) and [Apache Maven](https://maven.apache.org/install.html) installed.

From the parent project root, run the following command to run the application:

```shell
mvn -pl fullstack -P execute compile exec:exec
```

Then open a browser and navigate to <https://localhost:8443/>.

> **_Note:_**  consider checking the Disable Cache checkbox in the Network tab of the Web Developer Tools.

### Set up the project in an IDE

- [Eclipse IDE](https://eclipseide.org/):
  1. download the [Eclipse Installer](https://www.eclipse.org/downloads/packages/installer)
  2. install the package for Enterprise Java and Web Developers with JRE 25
  3. launch the IDE and choose Import projects from Git (with smart import)
  4. select GitHub as the repository source, then search for `janilla-blank-template` and complete the wizard
  5. open the Java class `com.janilla.blanktemplate.fullstack.BlankFullstack` and launch Debug as Java Application
  6. open a browser and navigate to <https://localhost:8443/>
- [Visual Studio Code](https://code.visualstudio.com/):
  1. download [Visual Studio Code](https://code.visualstudio.com/download)
  2. launch the IDE, install the [Oracle Java Platform Extension](https://marketplace.visualstudio.com/items?itemName=Oracle.oracle-java) and set up JDK 25
  3. open `Source Control,` select `Clone Repository`, select `Clone from GitHub` and search for "janilla-blank-template"
  4. select a destination folder and select `Open` the repository
  5. open `Explorer`, expand `Projects`, select `janilla-blank-template-fullstack` and select `Run Project Without Debugging`
  6. open a browser and navigate to <https://localhost:8443/>

> **_Note:_**  consider checking the Disable Cache checkbox in the Network tab of the Web Developer Tools.

### Where you can get help

Please visit [www.janilla.com](https://janilla.com/) for more information.

You can use [GitHub Issues](https://github.com/diego-schivo/janilla-blank-template/issues) to give or receive feedback.
