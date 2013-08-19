#!/bin/sh
JAVA=/opt/jdk1.7.0_10/bin/java
ulimit -n 100000

GC_OPTS=" -XX:+UseParNewGC -verbose:gc -XX:GCLogFileSize=10M  -Xloggc:logs/gc.log -XX:-UseGCLogFileRotation -XX:+PrintGCTimeStamps -XX:+PrintGCDetails -XX:+PrintTenuringDistribution -XX:+UseConcMarkSweepGC "

su evec -c "$JAVA -Xms6172M -Xmx6172M -Xss1M $GC_OPTS -jar target/eve-central-ng-assembly-3.1.4.jar" > /dev/null &
