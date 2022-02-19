resolvers += Resolver.url(
  "lila-maven-sbt",
  url("https://raw.githubusercontent.com/lichess-org/lila-maven/master")
)(Resolver.ivyStylePatterns)
addSbtPlugin("com.typesafe.play" % "sbt-plugin"   % "2.8.8-lila_1.8")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("ch.epfl.scala"     % "sbt-bloop"    % "1.4.13")
