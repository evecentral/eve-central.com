
resolvers ++= Seq (
  "zentrope" at "http://zentrope.com/maven"
)

addSbtPlugin("com.zentrope" %% "xsbt-scalate-precompile-plugin" % "1.7")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.7.2")

resolvers += "retronym-releases" at "http://retronym.github.com/repo/releases"

resolvers += "retronym-snapshots" at "http://retronym.github.com/repo/snapshots"

addSbtPlugin("com.github.retronym" % "sbt-onejar" % "0.7")
