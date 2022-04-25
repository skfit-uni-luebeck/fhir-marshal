# fhir-marshal

The FHIR-Marshal is a light-weight and easy-to-configure validation service for HL7© FHIR Resources.
The Marshal is based on Spring Boot and can validate against the FHIR base profiles and can query custom profiles from a FHIR Server. In addition, a Terminology server can be connected to validate used codes.


### Configuration.
To connect to the external server, the base URL needs to be added to the [application.yml](https://github.com/itcr-uni-luebeck/fhir-marshal/blob/main/src/main/resources/application.yml):

```yaml
fhir:
  remote-structure-server: "https://fhir.itcr.uni-luebeck.de/fhir/"
  remote-terminology-server: "https://terminology.itcr.uni-luebeck.de/fhir/"
```

### Deployment

The FHIR-Marshal can execute as a Java Jar or as a Docker Container. The existing [docker-compose](https://github.com/itcr-uni-luebeck/fhir-marshal/blob/main/docker-compose.yaml) file comes with a NGINX as a load balancer and two FHIR-Marshal instances for a scaling validation.

##### Single Instance using Java Jar

```bash
gradle bootJar
java -jar build/libs/fhir-marshal.jar
```

##### Scaling Validation using Docker-compose

```bash
docker-compose up
```
