# Organization Flapdoodle OSS

# Embedded MongoDB Spring Integration

This is an replacement for the spring mongodb integration project. It is based on Spring 2.7.x. This version uses a
new version of [Embedded MongoDB](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/).

## License

We use http://www.apache.org/licenses/LICENSE-2.0

### Maven

[![Maven Central](https://img.shields.io/maven-central/v/de.flapdoodle.embed/de.flapdoodle.embed.mongo.spring27x.svg)](https://maven-badges.herokuapp.com/maven-central/de.flapdoodle.embed/de.flapdoodle.embed.mongo.spring27x)

	<dependency>
		<groupId>de.flapdoodle.embed</groupId>
		<artifactId>de.flapdoodle.embed.mongo.spring27x</artifactId>
		<version>4.3.1</version>
	</dependency>

If you are getting some older version (< 4.x.x) for 'de.flapdoodle.embed.mongo', you must add the
dependency:

    <dependency>
         <groupId>de.flapdoodle.embed</groupId>
         <artifactId>de.flapdoodle.embed.mongo</artifactId>
         <version>4.2.0</version>
    </dependency>

### Usage

You might find an example for different use cases in this [documentation](HowTo.md). As this documentation is generated
on each build by running this code, it should work as expected:)


