
resolvers ++= Seq (
  "zentrope" at "http://zentrope.com/maven"
)

addSbtPlugin("com.zentrope" %% "xsbt-scalate-precompile-plugin" % "1.7")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.7.2")
