resolvers += Resolver.url(
  "lila-maven-sbt",
  // sbt 2.0's `url(...)` returns a URI; Resolver.url wants a java.net.URL.
  new java.net.URI("https://raw.githubusercontent.com/lichess-org/lila-maven/master").toURL
)(Resolver.ivyStylePatterns)

// sbt 2.0 Play plugin (ported). Currently resolved from the local ivy repo produced by
// playframework-lila/publish-sbt2; will move to lila-maven once that artifact is published there.
resolvers += Resolver.mavenLocal
addSbtPlugin("org.lichess.play" % "sbt-plugin" % "2.0.0-RC2")

// The Play plugin's RoutesCompiler/PlayLayout autoplugins require these; under sbt 2.0 the
// transitive plugin activation differs from sbt 1.x, so declare them explicitly.
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.4")
addSbtPlugin("org.playframework.twirl" % "sbt-twirl" % "2.1.0-M9")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.1")
