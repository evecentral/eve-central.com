import AssemblyKeys._ // put this at the top of the file                                                                                                                                                                                            
import com.mojolly.scalate.ScalatePlugin._

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

version := "3.0.3"

scalaVersion := "2.9.1"

resolvers += "Twitter" at "http://maven.twttr.com/"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Akka Repository" at "http://akka.io/repository/"

resolvers += "Spray" at "http://repo.spray.cc/"

resolvers += "Codehale" at "http://repo.codahale.com"

resolvers += "theatr.us" at "http://repo.theatr.us"

libraryDependencies += "com.codahale" %% "jerkson" % "0.5.0"

libraryDependencies += "se.scalablesolutions.akka" % "akka-actor" % "1.3.1"

libraryDependencies += "se.scalablesolutions.akka" % "akka-stm" % "1.3.1"

libraryDependencies += "se.scalablesolutions.akka" % "akka-testkit" % "1.3.1"

libraryDependencies += "se.scalablesolutions.akka" % "akka-slf4j" % "1.3.1"

libraryDependencies += "net.noerd" %% "prequel" % "0.3.8" changing()

libraryDependencies += "org.scalatest" %% "scalatest" % "1.7.2" % "test"

libraryDependencies += "postgresql" % "postgresql" % "9.1-901.jdbc4"

libraryDependencies += "cc.spray" % "spray-server" % "0.9.0"

libraryDependencies += "cc.spray" % "spray-client" % "0.9.0"

libraryDependencies += "net.liftweb" %% "lift-json" % "2.4"

libraryDependencies += "cc.spray" % "spray-can" % "0.9.3"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.6.4"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.3"

libraryDependencies += "org.fusesource.scalate" % "scalate-core" % "1.5.3"

libraryDependencies += "cc.spray" %% "spray-json" % "1.2.0-SNAPSHOT" % "compile" changing() withSources()

libraryDependencies += "net.sf.jung" % "jung-algorithms" % "2.0.1"

libraryDependencies += "net.sf.jung" % "jung-graph-impl" % "2.0.1"

libraryDependencies += "org.scalaj" %% "scalaj-collection" % "1.2"

libraryDependencies += "javax.mail" % "mail" % "1.4.4"

libraryDependencies += "org.zeromq" %% "zeromq-scala-binding" % "0.0.5"

libraryDependencies += "com.jcraft" % "jzlib" % "1.1.1"

libraryDependencies += "commons-collections" % "commons-collections" % "3.2.1"


seq(scalateSettings:_*)

scalateTemplateDirectory in Compile <<= (baseDirectory) { _ / "src/main/resources/com" }









