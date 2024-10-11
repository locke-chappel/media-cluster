FROM alpine:latest AS download

ARG APP_VERSION=0.1.0
ARG GPG_ID=E73F62163B32F6E2

USER root

RUN apk --no-cache add \
      gpg \
      gpg-agent && \
    wget -O /app.jar "https://github.com/locke-chappel/media-cluster/releases/download/${APP_VERSION}/scheduler-${APP_VERSION}.jar" && \
    wget -O /app.jar.asc "https://github.com/locke-chappel/media-cluster/releases/download/${APP_VERSION}/scheduler-${APP_VERSION}.jar.asc" && \
    gpg --keyserver keyserver.ubuntu.com --recv-keys $GPG_ID && \
    gpg --verify /app.jar.asc /app.jar && \
    if [[ $? != 0 ]]; then \
      echo "GPG Signature Failed, aborting"; \
      exit 100; \
    fi

FROM alpine:latest

USER root

RUN apk --no-cache add \
      openjdk17-jre-headless && \
    adduser --gecos ,,,,, --uid 9001 --disabled-password user && \
    passwd -d user && \
    passwd -l user && \
    sed -i "s|user:!|user:*|g" /etc/shadow && \
    mkdir /home/user/cache && \
    mkdir -p /home/user/ephemeral && \
    mkdir -p /home/user/app-data

COPY --from=download /app.jar /home/user/app.jar
COPY ./entrypoint /home/user/entrypoint

RUN chown -R user:user /home/user && \
    chmod -R 0700 /home/user

USER user

VOLUME /home/user/app-data
VOLUME /home/user/ephemeral

ENTRYPOINT [ "/home/user/entrypoint" ]
