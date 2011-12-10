

name := "eve-central-ng"

version := "3.0"

scalaVersion := "2.9.1"

resolvers += "Twitter" at "http://maven.twttr.com/"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"


libraryDependencies += "se.scalablesolutions.akka" % "akka-actor" % "1.2"

libraryDependencies += "com.twitter" % "querulous" % "2.1.5"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.6.1" % "test"

libraryDependencies += "postgresql" % "postgresql" % "9.1-901.jdbc4"

