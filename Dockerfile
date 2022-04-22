FROM openjdk:11
CMD ["gradle", "bootJar"]
COPY build/libs/fhir-marshal.jar fhir-marshal.jar
ENTRYPOINT ["java","-jar","fhir-marshal.jar"]