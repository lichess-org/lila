resolvers += Resolver.url(
  "lila-maven-sbt",
  // sbt 2.0's `url(...)` returns a URI; Resolver.url wants a java.net.URL.
  new java.net.URI("https://raw.githubusercontent.com/lichess-org/lila-maven/master").toURL
)(Resolver.ivyStylePatterns)

addSbtPlugin("org.lichess.play" % "sbt-plugin" % "2.0.0-RC2")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.4")
addSbtPlugin("org.playframework.twirl" % "sbt-twirl" % "2.1.0-M9")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.1")
