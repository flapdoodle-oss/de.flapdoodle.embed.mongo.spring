# Usage

You must disable the auto configuration provided by spring by disabling the spring provided
auto configuration class:

```java
@DataMongoTest(
  excludeAutoConfiguration = org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration.class
)
@ExtendWith(SpringExtension.class)
public class AutoConfigTest {
  @Test
  void example(@Autowired final MongoTemplate mongoTemplate) {
    Assertions.assertThat(mongoTemplate.getDb()).isNotNull();
  }
}
```

## Test Isolation

Per default there is just one mongodb instance running. In case you need test isolation you can annotate your test
with `@TestPropertySource` as in this example:

```java
@DataMongoTest(
  excludeAutoConfiguration = org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration.class
)
@TestPropertySource(properties = "property=A")
@ExtendWith(SpringExtension.class)
@DirtiesContext
public class AutoConfigFirstIsolationTest {
  @Test
  void example(@Autowired final MongoTemplate mongoTemplate) {
    mongoTemplate.getDb().createCollection("deleteMe");
    long count = mongoTemplate.getDb().getCollection("deleteMe").countDocuments(Document.parse("{}"));

    assertThat(mongoTemplate.getDb()).isNotNull();
    assertThat(count).isEqualTo(0L);
  }
}
```

The tests with the same configuration will share their instance. If you want to achive test isolation with the same
configuration you must annotate your test with `@DirtiesContext` so that this test will have his own mongodb:

```java
@DataMongoTest(
  excludeAutoConfiguration = org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration.class
)
@TestPropertySource(properties = "property=A")
@ExtendWith(SpringExtension.class)
@DirtiesContext
public class AutoConfigSecondIsolationTest {
  @Test
  void example(@Autowired final MongoTemplate mongoTemplate) {
    mongoTemplate.getDb().createCollection("deleteMe");
    long count = mongoTemplate.getDb().getCollection("deleteMe").countDocuments(Document.parse("{}"));

    assertThat(mongoTemplate.getDb()).isNotNull();
    assertThat(count).isEqualTo(0L);
  }
}
```

## Test migration

You should change the mongodb relevant prefix in your config files from `spring.mongodb.embedded` to 'de.flapdoodle.mongodb.embedded'.