lazy val plugins = (project in file(".")).settings(
  scalaVersion := "2.12.17" // TODO: remove when upgraded to sbt 1.8.0, see https://github.com/sbt/sbt/pull/7021
)
resolvers += Resolver.url(
  "lila-maven-sbt",
  url("https://raw.githubusercontent.com/lichess-org/lila-maven/master")
)(Resolver.ivyStylePatterns)
addSbtPlugin("com.typesafe.play" % "sbt-plugin"   % "2.8.16-lila_1.14")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("ch.epfl.scala"     % "sbt-bloop"    % "1.5.3")
// addDependencyTreePlugin
