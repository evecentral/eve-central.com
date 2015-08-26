#!/bin/sh

JAVA=/opt/jdk1.8.0_45/bin/java
ulimit -n 1000000

GC_OPTS="-XX:+UseParNewGC -verbose:gc -XX:GCLogFileSize=10M  -Xloggc:logs/gc.log -XX:-UseGCLogFileRotation -XX:+PrintGCTimeStamps -XX:+PrintGCDetails -XX:+PrintTenuringDistribution -XX:+UseConcMarkSweepGC "

su evec -c "$JAVA  -XX:+UseAdaptiveSizePolicy -Xmx23g $GC_OPTS -jar target/scala-2.10/eve-central-ng-assembly-3.2.0.jar" > /dev/null &
