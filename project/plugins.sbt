resolvers += Resolver.url(
  "lila-maven-sbt",
  url("https://raw.githubusercontent.com/ornicar/lila-maven/master")
)(Resolver.ivyStylePatterns)
addSbtPlugin("com.typesafe.play" % "sbt-plugin"   % "2.8.2-lila_1.4")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("ch.epfl.scala"     % "sbt-bloop"    % "1.4.5")
