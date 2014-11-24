#!/bin/sh
JAVA=/opt/jdk1.7.0_67/bin/java
ulimit -n 100000

GC_OPTS=" -XX:+UseParNewGC -verbose:gc -XX:GCLogFileSize=10M  -Xloggc:logs/gc.log -XX:-UseGCLogFileRotation -XX:+PrintGCTimeStamps -XX:+PrintGCDetails -XX:+PrintTenuringDistribution -XX:+UseConcMarkSweepGC "

su evec -c "$JAVA -Xms10g -Xmx10g -Xss1M $GC_OPTS -jar target/scala-2.10/eve-central-ng-assembly-3.1.6.jar" > /dev/null &
