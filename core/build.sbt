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

version := "3.1.2"

scalaVersion := "2.9.2"

scalacOptions += "-Ydependent-method-types"

resolvers += "Twitter" at "http://maven.twttr.com/"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "akka repo" at "http://repo.akka.io/releases/"

resolvers += "Spray" at "http://repo.spray.io/"

resolvers += "Spray Nightlies" at "http://nightlies.spray.io/"

resolvers += "theatr.us" at "http://repo.theatr.us"

libraryDependencies += "io.spray" % "spray-can"     % "1.0-M7"

libraryDependencies += "io.spray" % "spray-routing"     % "1.0-M7"

libraryDependencies +="io.spray" % "spray-testkit"     % "1.0-M7"

libraryDependencies += "com.typesafe.akka" % "akka-actor"  % "2.0.4"

libraryDependencies += "com.typesafe.akka" % "akka-testkit" % "2.0.4"

libraryDependencies += "net.noerd" %% "prequel" % "0.3.8" changing()

libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test"

libraryDependencies += "postgresql" % "postgresql" % "9.1-901.jdbc4"

libraryDependencies += "net.liftweb" %% "lift-json" % "2.5-SNAPSHOT"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.6.4"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.3"

libraryDependencies += "com.google.guava" % "guava" % "13.0"

libraryDependencies += "com.google.code.findbugs" % "jsr305" % "1.3.+"

libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala" % "2.1.2"

libraryDependencies += "net.sf.jung" % "jung-algorithms" % "2.0.1"

libraryDependencies += "net.sf.jung" % "jung-graph-impl" % "2.0.1"

libraryDependencies += "javax.mail" % "mail" % "1.4.4"

libraryDependencies += "commons-collections" % "commons-collections" % "3.2.1"









