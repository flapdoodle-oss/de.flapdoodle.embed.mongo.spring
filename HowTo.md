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