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

## Config Prefix

Use '${noop.prefix}' as prefix in your config files.