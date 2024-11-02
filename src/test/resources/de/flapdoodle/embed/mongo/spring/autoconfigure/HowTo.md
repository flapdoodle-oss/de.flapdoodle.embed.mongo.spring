# Usage

You must disable the auto configuration provided by spring by disabling the spring provided
auto configuration class:

```java
${autoConfigClass}
```

## Test Isolation

Per default there is just one mongodb instance running. In case you need test isolation you can annotate your test
with `@TestPropertySource` as in this example:

```java
${firstIsolation}
```

The tests with the same configuration will share their instance. If you want to achive test isolation with the same
configuration you must annotate your test with `@DirtiesContext` so that this test will have his own mongodb:

```java
${secondIsolation}
```

## Json Import
                        
If you create a bean config for a list of `MongoImportArguments` a mongoimport process is started as soon as the mongodb is running but before
any test code is executed.
If `mongoimport` is not bundled within the mongodb version, then you have to define a tools version: '${noop.prefix}.tools-version'.

```java
${importJsonClass}
```

## Transactions

To enable transactions with spring data, there is one minimal setup. Imagine you have an person repository:                 

```java
${transaction.repository}
```

... which is called inside this service:

```java
${transaction.service}
```

Then you must enable an MongoTransactionManager:

```java
${transaction.config}
```

... and if we call the service from test, some calls will succeed and others will fail so that
the transaction is not committed:

```java
${transaction.test}
```

## Custom Database Dir

```java
${customDatabaseDir}
```

## Customize Mongod

If none of the other configuration options is enough, you can customize it further by adding a `BeanPostProcessor` or
use a more typesafe implementation like `TypedBeanPostProcessor:

```java
${customizeMongod.config}
```

```java
${customizeMongod}
```

## Config Prefix

Use '${noop.prefix}' as prefix in your config files.

## Test with a real database

If you want to test with an 'unmanaged' mongodb database, you must disable the auto configuration by excluding EmbeddedMongoAutoConfiguration:

```java
@EnableAutoConfiguration(exclude = {EmbeddedMongoAutoConfiguration.class})
```
