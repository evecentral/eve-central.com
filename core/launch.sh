#!/bin/sh
JAVA=/opt/jdk1.7.0_10/bin/java
ulimit -n 100000
su evec -c "$JAVA -Xms512M -Xmx6172M -Xss1M -XX:+UseConcMarkSweepGC -jar target/eve-central-ng-assembly-3.1.0.jar"  
