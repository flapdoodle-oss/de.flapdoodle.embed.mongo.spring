# Organization Flapdoodle OSS

# Embedded MongoDB Spring Integration

This is a replacement for the spring mongodb integration project. It is based on Spring 2.7.x. This version uses a
new version of [Embedded MongoDB](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/).

## License

We use http://www.apache.org/licenses/LICENSE-2.0

### Maven

[![Maven Central](https://img.shields.io/maven-central/v/de.flapdoodle.embed/de.flapdoodle.embed.mongo.spring27x.svg)](https://maven-badges.herokuapp.com/maven-central/de.flapdoodle.embed/de.flapdoodle.embed.mongo.spring27x)

	<dependency>
		<groupId>de.flapdoodle.embed</groupId>
		<artifactId>de.flapdoodle.embed.mongo.spring27x</artifactId>
		<version>4.12.3</version>
	</dependency>

If you are getting some older version (< 4.x.x) for 'de.flapdoodle.embed.mongo', you must add the
dependency ( [![Maven Central](https://img.shields.io/maven-central/v/de.flapdoodle.embed/de.flapdoodle.embed.mongo.svg)](https://maven-badges.herokuapp.com/maven-central/de.flapdoodle.embed/de.flapdoodle.embed.mongo) ):

    <dependency>
         <groupId>de.flapdoodle.embed</groupId>
         <artifactId>de.flapdoodle.embed.mongo</artifactId>
         <version>4.12.3</version>
    </dependency>

You can use this dependency with any spring 2.7.x version.

### Usage

You might find an example for different use cases in this [documentation](HowTo.md). As this documentation is generated
on each build by running this code, it should work as expected:)

### Spring Gradle Plugin io.spring.dependency-management

The Spring gradle plugin `io.spring.dependency-management` sets a plethora of dependencies to particular versions.
One of its dependency management coordinates is `de.flapdoodle.embed:de.flapdoodle.embed.mongo:3.4.11`.
The recommended way to overwrite this dependency management coordinate set by the Spring gradle plugin
`io.spring.dependency-management` is to add the following code block to your `build.gradle`:
```
dependencyManagement {
    dependencies {
        dependency group:'de.flapdoodle.embed', name:'de.flapdoodle.embed.mongo', version:'4.12.3'
    }
}
```
You can then check whether this was successful by listing the dependency management coordinates managed by the
Spring gradle plugin `io.spring.dependency-management`:
```
gradle dependencyManagement
```
This is no longer an issue in Spring Boot version 3.0.0 and later as the dependency management coordinate for
`de.flapdoodle.embed:de.flapdoodle.embed.mongo` was removed.  

### Canary Project

To see if all works as expected and as a playground for integration problems you may have a look at
[flapdoodle embed mongo canary](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo.canary). There you will
find a minimal example for spring2.6.x, spring2.7.x , spring3.x.x (hint: spring3.x.x needs java17). 
