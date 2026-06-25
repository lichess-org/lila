resolvers += Resolver.url(
  "lila-maven-sbt",
  new java.net.URL("https://raw.githubusercontent.com/lichess-org/lila-maven/master")
)(using Resolver.ivyStylePatterns)

addSbtPlugin("org.lichess.play" % "sbt-plugin" % "2.0.0-RC2")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.4")
addSbtPlugin("org.playframework.twirl" % "sbt-twirl" % "2.1.0-M9")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.1")
