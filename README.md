# Organization Flapdoodle OSS

# Embedded MongoDB Spring Integration

This is a replacement for the spring mongodb integration project. It is based on Spring 2.5.x. This version uses a
new version of [Embedded MongoDB](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/).

## License

We use http://www.apache.org/licenses/LICENSE-2.0

### Maven

[![Maven Central](https://img.shields.io/maven-central/v/de.flapdoodle.embed/de.flapdoodle.embed.mongo.spring25x.svg)](https://maven-badges.herokuapp.com/maven-central/de.flapdoodle.embed/de.flapdoodle.embed.mongo.spring25x)

	<dependency>
		<groupId>de.flapdoodle.embed</groupId>
		<artifactId>de.flapdoodle.embed.mongo.spring25x</artifactId>
		<version>4.20.0</version>
	</dependency>

If you are getting some older version (< 4.x.x) for 'de.flapdoodle.embed.mongo', you must add the
dependency ( [![Maven Central](https://img.shields.io/maven-central/v/de.flapdoodle.embed/de.flapdoodle.embed.mongo.svg)](https://maven-badges.herokuapp.com/maven-central/de.flapdoodle.embed/de.flapdoodle.embed.mongo) ):

    <dependency>
         <groupId>de.flapdoodle.embed</groupId>
         <artifactId>de.flapdoodle.embed.mongo</artifactId>
         <version>4.20.1</version>
    </dependency>

You can use this dependency with any spring 2.5.x version.

To enable logging you must choose some matching adapter for [slf4j.org](https://www.slf4j.org/) This projects uses slf4j-api version 1.7.xx.

### Usage

You might find an example for different use cases in this [documentation](HowTo.md). As this documentation is generated
on each build by running this code, it should work as expected:)

### Canary Project

To see if all works as expected and as a playground for integration problems you may have a look at
[flapdoodle embed mongo canary](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo.canary). There you will
find a minimal example for spring2.6.x, spring2.7.x , spring3.x.x (hint: spring3.x.x needs java17). 
