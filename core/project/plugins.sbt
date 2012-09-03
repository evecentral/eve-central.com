
resolvers ++= Seq (
  "zentrope" at "http://zentrope.com/maven"
)

resolvers += Resolver.url("sbt-plugin-releases",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)



addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.3")

resolvers ++= Seq( "sbt-plugin-releases" at "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/")


addSbtPlugin("com.untyped" % "sbt-js"      % "0.4")

addSbtPlugin("com.untyped" % "sbt-less"    % "0.4")


