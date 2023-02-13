#!/bin/sh
./mvnw clean install
./mvnw release:clean
./mvnw release:prepare -DskipTests -Darguments=-DskipTests
./mvnw release:perform -DskipTests -Darguments=-DskipTests

