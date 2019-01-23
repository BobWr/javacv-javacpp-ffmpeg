FROM alpine:3.8 AS base

RUN apk add --no-cache --update \
    libgcc \
    libstdc++ \
    ca-certificates \
    libcrypto1.0 \
    libssl1.0 \
    libgomp \
    expat \
    pulseaudio-dev \
    libxcb

### Release Stage
FROM base AS release

LABEL author="Zukdoor Inc."

RUN apk add --no-cache shadow sudo chromium xvfb pulseaudio pulseaudio-utils openjdk8 maven

RUN apk add --no-cache --update -X http://dl-cdn.alpinelinux.org/alpine/edge/testing alsa-plugins

RUN sed '/module-console-kit/,/.endif/d; /module-systemd-login/,/.endif/d' -i /etc/pulse/default.pa

ENV UNAME zukvnc

RUN export UNAME=$UNAME UID=1000 GID=1000 && \
    mkdir -p "/home/${UNAME}" && \
    echo "${UNAME}:x:${UID}:${GID}:${UNAME} User,,,:/home/${UNAME}:/bin/bash" >> /etc/passwd && \
    echo "${UNAME}:x:${UID}:" >> /etc/group && \
    mkdir -p /etc/sudoers.d && \
    echo "${UNAME} ALL=(ALL) NOPASSWD: ALL" > /etc/sudoers.d/${UNAME} && \
    chmod 0440 /etc/sudoers.d/${UNAME} && \
    chown ${UID}:${GID} -R /home/${UNAME} && \
    gpasswd -a ${UNAME} audio

USER $UNAME
ENV HOME /home/zukvnc
WORKDIR $HOME
COPY zukvnc/.asoundrc .
COPY zukvnc/yahei.ttf /usr/share/fonts/TTF

#安装noVNC,x11vnc
RUN sudo apk add --no-cache git x11vnc py-numpy
RUN git clone https://github.com/novnc/noVNC.git
RUN git clone https://github.com/novnc/websockify noVNC/utils/websockify
RUN sed -i 's/bash/sh/' noVNC/utils/launch.sh
RUN sudo apk add --no-cache procps


COPY zukvnc/zukalp.sh /home/zukvnc/zukalp.sh
RUN sudo chmod +x zukalp.sh

ENTRYPOINT [ "sh", "-c", "./zukalp.sh" ]