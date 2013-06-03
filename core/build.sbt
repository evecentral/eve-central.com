import AssemblyKeys._ // put this at the top of the file

assemblySettings

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>                                                                                                                                                                                 {
	case "META-INF/NOTICE.txt" => MergeStrategy.discard
	case "META-INF/LICENSE.txt" => MergeStrategy.discard
	case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
	case "META-INF\\NOTICE.txt" => MergeStrategy.discard
	case "META-INF\\LICENSE.txt" => MergeStrategy.discard
	case "META-INF/MANIFEST.MF" => MergeStrategy.discard
	case "META-INF/BCKEY.SF" => MergeStrategy.discard
	case "META-INF\\BCKEY.SF" => MergeStrategy.discard
	case x => old(x)
  }
}


name := "eve-central-ng"

version := "3.1.4"

scalaVersion := "2.10.1"

resolvers ++= Seq(
  "Twitter" at "http://maven.twttr.com/",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
  "akka repo" at "http://repo.akka.io/releases/",
  "Spray" at "http://repo.spray.io/",
  "Spray Nightlies" at "http://nightlies.spray.io/",
  "theatr.us" at "http://repo.theatr.us")

libraryDependencies ++= Seq(
  "io.spray" % "spray-can" % "1.1-M7",
  "io.spray" % "spray-routing"     % "1.1-M7",
  "io.spray" % "spray-testkit"     % "1.1-M7",
  "com.typesafe.akka" %% "akka-actor"  % "2.1.4",
  "com.typesafe.akka" %% "akka-testkit" % "2.1.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.1.4",
  "net.noerd" %% "prequel" % "0.3.9",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "net.liftweb" %% "lift-json" % "2.5-SNAPSHOT",
  "org.slf4j" % "slf4j-api" % "1.6.4",
  "ch.qos.logback" % "logback-classic" % "1.0.3",
  "com.google.guava" % "guava" % "14.0",
  "com.google.code.findbugs" % "jsr305" % "1.3.+",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.2.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.0",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.2.0",
  "net.sf.jung" % "jung-algorithms" % "2.0.1",
  "net.sf.jung" % "jung-graph-impl" % "2.0.1",
  "javax.mail" % "mail" % "1.4.4",
  "commons-collections" % "commons-collections" % "3.2.1",
  "com.github.spullara.mustache.java" % "compiler" % "0.8.12")

publishMavenStyle := true

publishTo := Some(Resolver.file("file", new File("../../ivy-repo/")))






