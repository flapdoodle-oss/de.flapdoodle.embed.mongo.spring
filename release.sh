#!/bin/sh
hint() {
	echo "*********************************************"
	echo "* as mvn release increments just the number *"
	echo "* in any version string make sure that you  *"
	echo "* change the version                        *"
	echo "* not from spring2x to spring2(x+1)         *"
	echo "*********************************************"
}

./mvnw clean install
./mvnw release:clean
hint
./mvnw release:prepare -DskipTests -Darguments=-DskipTests
hint
./mvnw release:perform -DskipTests -Darguments=-DskipTests

