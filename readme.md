This project contains the sources of and is the common base for both SWC and STQM.

Some remarks about running build in Jenkins with JDK11 (and in Docker)

# Configuring Jenkins pipeline for integration testing in a separate stage 
keeping all maven goal configuration in one pom file causes the unit tests also be re-executed when
you want to run only the integration tests in a separate pipeline step.
To void this, a separate pom-integrationTestig.xml was added. In this file the unit tests are configured to be skipped, when running the integration testing.
Note: you could also remove the unit test plugin completely, but then maven still tries to run the unit tests.
 

1. Runs with JDK 10

2. Surefire plugin
Switch to new version and some extra configuration to avoid crash
