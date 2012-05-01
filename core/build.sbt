

name := "eve-central-ng"

version := "3.0"

scalaVersion := "2.9.1"

resolvers += "Twitter" at "http://maven.twttr.com/"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Akka Repository" at "http://akka.io/repository/"

resolvers += "Spray" at "http://repo.spray.cc/"

libraryDependencies += "se.scalablesolutions.akka" % "akka-actor" % "1.3.1"

libraryDependencies += "se.scalablesolutions.akka" % "akka-stm" % "1.3.1"

libraryDependencies += "se.scalablesolutions.akka" % "akka-testkit" % "1.3.1"

libraryDependencies += "se.scalablesolutions.akka" % "akka-slf4j" % "1.3.1"

libraryDependencies += "net.noerd" %% "prequel" % "0.3.8" changing()

libraryDependencies += "org.scalatest" %% "scalatest" % "1.6.1" % "test"

libraryDependencies += "postgresql" % "postgresql" % "9.1-901.jdbc4"

libraryDependencies += "cc.spray" % "spray-server" % "0.9.0-RC3"

libraryDependencies += "cc.spray" % "spray-client" % "0.9.0-RC3"

//libraryDependencies += "net.liftweb" % "lift-json" % "2.0"

libraryDependencies += "cc.spray" % "spray-can" % "0.9.3"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.6.1"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "0.9.29"

libraryDependencies += "org.fusesource.scalate" % "scalate-core" % "1.5.3"

libraryDependencies += "cc.spray" %% "spray-json" % "1.2.0-SNAPSHOT" % "compile" changing() withSources()

libraryDependencies += "net.sf.jung" % "jung-algorithms" % "2.0.1"

libraryDependencies += "net.sf.jung" % "jung-graph-impl" % "2.0.1"

libraryDependencies += "org.scalaj" %% "scalaj-collection" % "1.2"

libraryDependencies += "javax.mail" % "mail" % "1.4.4"


seq(com.zentrope.ScalatePlugin.scalateTemplateSettings : _*)

seq(assemblySettings: _*)

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)





