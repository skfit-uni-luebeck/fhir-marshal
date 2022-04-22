FROM openjdk:11
COPY build/libs/fhir-marshal-0.0.1.jar fhir-marshal.jar
ENTRYPOINT ["java","-jar","fhir-marshal.jar"]