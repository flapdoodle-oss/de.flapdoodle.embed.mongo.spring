# Organization Flapdoodle OSS

# Embedded MongoDB Spring Integration

[![build](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo.spring/actions/workflows/build.yml/badge.svg?branch=spring-3.1.x)](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo.spring/actions/workflows/build.yml)

This is an spring mongodb integration project. This version uses a
new version of [Embedded MongoDB](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/).

## License

We use http://www.apache.org/licenses/LICENSE-2.0

### Maven

[![Maven Central](https://img.shields.io/maven-central/v/de.flapdoodle.embed/de.flapdoodle.embed.mongo.spring31x.svg)](https://maven-badges.herokuapp.com/maven-central/de.flapdoodle.embed/de.flapdoodle.embed.mongo.spring31x)

	<dependency>
		<groupId>de.flapdoodle.embed</groupId>
		<artifactId>de.flapdoodle.embed.mongo.spring3x</artifactId>
		<version>4.14.0</version>
	</dependency>

You can use this dependency with any spring 3.x.x version.

### Usage

You might find an example for different use cases in this [documentation](HowTo.md). As this documentation is generated
on each build by running this code, it should work as expected:)

### Canary Project

To see if all works as expected and as a playground for integration problems you may have a look at
[flapdoodle embed mongo canary](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo.canary). There you will
find a minimal example for spring2.6.x, spring2.7.x , spring3.x.x (hint: spring3.x.x needs java17). 
