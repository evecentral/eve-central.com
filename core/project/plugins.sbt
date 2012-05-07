
resolvers ++= Seq (
  "zentrope" at "http://zentrope.com/maven"
)

resolvers += Resolver.url("sbt-plugin-releases",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)


libraryDependencies <+= sbtVersion(v => "com.mojolly.scalate" %% "xsbt-scalate-generator" % (v + "-0.1.6"))


addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.0")
