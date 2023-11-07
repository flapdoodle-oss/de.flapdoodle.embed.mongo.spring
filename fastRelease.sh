#!/bin/sh

./mvnw clean install -DskipTests -Darguments=-DskipTests
./mvnw release:clean -DskipTests -Darguments=-DskipTests
./mvnw release:prepare -DskipTests -Darguments=-DskipTests
./mvnw release:perform -DskipTests -Darguments=-DskipTests

