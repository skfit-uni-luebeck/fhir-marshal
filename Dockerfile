FROM gradle:6.9.2-jdk11-alpine AS gradle_build
COPY . /home/fhir-marshall
WORKDIR /home/fhir-marshall

RUN gradle bootJar --no-daemon

ENV STRUCTURE_SERVER_URL: "http://localhost:8080/fhir"
ENV TERMINOLOGY_SERVER_URL: "https://ontoserver.imi.uni-luebeck.de/fhir"
ENV PORT: "8081"

FROM openjdk:11
COPY --from=gradle_build /home/fhir-marshall/build/libs/fhir-marshal.jar fhir-marshal.jar
ENTRYPOINT ["java", "-Dserver.port=${PORT}","-jar","fhir-marshal.jar"]