FROM gradle:6.9.2-jdk11-alpine AS gradle_build
COPY . /home/fhir-marshall
WORKDIR /home/fhir-marshall

RUN gradle bootJar --no-daemon

FROM openjdk:11
COPY --from=gradle_build /home/fhir-marshall/build/libs/fhir-marshal.jar fhir-marshal.jar
ENTRYPOINT ["java","-jar","fhir-marshal.jar"]