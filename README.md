# Hit UCC

Distributable UCC Discovery Algorithm based on Akka

## Requirements

In order to build and execute the code, you will need Java 8 and Maven.
To make sure that your project is set up correctly in an IDE, you can run the tests in the `akka-tutorial/src/test/java` folder. If you are operating from a command line instead, run `mvn test` in the folder with `pom.xml` file.

## Execution instructions

Just run the main class `de.hpi.hitucc.HitUCCApp`, respectively, from within your IDE or from the command line. The app will then print an overview of the different possible parameters. Append parameters of your choice to the run configuration in your IDE or to your command line call, as exemplified below:
* Parameters to start a master with two local workers: `master --workers 2`
* Parameters to start a slave that tries to connect to a remote master: `slave --master <master host>:<master port>`

