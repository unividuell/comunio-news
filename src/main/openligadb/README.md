# Generator

OpenAPI Generator CLI from sources: https://github.com/OpenAPITools/openapi-generator/wiki/FAQ#how-to-test-with-the-latest-master-of-openapi-generator

```
clemens@bender-m1m [16:00:56] [/tmp/openapi-generator] [master]
-> rm -rf /tmp/local/* && java -jar modules/openapi-generator-cli/target/openapi-generator-cli.jar generate -g kotlin -i https://api.openligadb.de/swagger/v1/swagger.json -o /tmp/local --additional-properties=library=jvm-spring-restclient,useSpringBoot3=true,serializationLibrary=jackson
```