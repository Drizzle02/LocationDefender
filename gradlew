#!/bin/sh
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export PATH=$JAVA_HOME/bin:$PATH
exec gradle "$@"
