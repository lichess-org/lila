resolvers += Resolver.url(
  "lila-maven-sbt",
  url("https://raw.githubusercontent.com/ornicar/lila-maven/master")
)(Resolver.ivyStylePatterns)
addSbtPlugin("com.typesafe.play" % "sbt-plugin"   % "2.8.8-lila_1.7")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt" % "2.4.3")
addSbtPlugin("ch.epfl.scala"     % "sbt-bloop"    % "1.4.8-112-64d8184c")
