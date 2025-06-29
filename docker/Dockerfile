FROM xqs2018/ubuntu-jdk:21
COPY ./gbtmedia-0.0.1-SNAPSHOT.jar /var/gbtmedia/jar/
CMD ["java","-jar","/var/gbtmedia/jar/gbtmedia-0.0.1-SNAPSHOT.jar","--spring.profiles.active=local"]

# docker buildx build --platform linux/amd64,linux/arm64 -t xqs2018/gbtmedia --push .
