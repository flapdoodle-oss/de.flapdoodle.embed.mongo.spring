package de.flapdoodle.embed.mongo.spring.autoconfigure;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest()
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
	"de.flapdoodle.mongodb.embedded.version=3.6.5"
	,"spring.data.mongodb.uri=mongodb://localhost/test"
})
public class SpecifyMongoConnectionTest {
	@Test
	void example(@Autowired final MongoTemplate mongoTemplate) {
		assertThat(mongoTemplate.getDb()).isNotNull();
		ArrayList<String> names = mongoTemplate.getDb()
			.listCollectionNames()
			.into(new ArrayList<>());

		assertThat(names).isEmpty();
	}
}
