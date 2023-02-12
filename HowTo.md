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

## Transactions

To enable transactions with spring data, there is one minimal setup. Imagine you have an person repository:                 

```java
public interface PersonRepository extends MongoRepository<Person,String> {
}
```

... which is called inside this service:

```java
@Service
public class PersonService {

  private PersonRepository repository;
  
  @Autowired
  public PersonService(PersonRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public void insert(String ... names) {
    for (String name : names) {
      repository.insert(new Person(name.substring(0, 1), name));
    }
  }

  public long count() {
    return repository.count();
  }
}
```

Then you must enable an MongoTransactionManager:

```java
@Configuration
public class TransactionalConfig {

  @Bean
  MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory dbFactory) {
    return new MongoTransactionManager(dbFactory);
  }

  @Bean
  MongodArguments mongodArguments() {
    return MongodArguments.builder()
      .replication(Storage.of("test", 10))
      .build();
  }
}
```

... and if we call the service from test, some calls will succeed and others will fail so that
the transaction is not committed:

```java
@AutoConfigureDataMongo
@SpringBootTest(
  properties = "de.flapdoodle.mongodb.embedded.version=5.0.5"
)
@EnableAutoConfiguration(
  exclude = org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration.class
)
@TestPropertySource(properties = "property=C")
@DirtiesContext
public class TransactionalTest {

  @Test
  void personExample(@Autowired PersonService service) {
    service.insert("Klaus","Susi");

    assertThat(service.count()).isEqualTo(2);

    assertThatThrownBy(() -> service.insert("Helga","Hans"))
      .isInstanceOf(RuntimeException.class);

    assertThat(service.count()).isEqualTo(2);
  }
}
```

## Custom Database Dir

```java
@AutoConfigureDataMongo
@SpringBootTest(
  properties = {
    "de.flapdoodle.mongodb.embedded.databaseDir=${java.io.tmpdir}/customDir/${random.uuid}"
  }
)
@EnableAutoConfiguration(
  exclude = org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration.class
)
@DirtiesContext
public class CustomDatabaseDirTest {

  @Test
  void example(@Autowired final MongoTemplate mongoTemplate) {
    Assertions.assertThat(mongoTemplate.getDb()).isNotNull();
  }
}
```


## Config Prefix

Use 'de.flapdoodle.mongodb.embedded' as prefix in your config files.