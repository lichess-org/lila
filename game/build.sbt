name := "lila-game"

organization := "org.lichess"

scalaVersion := "2.10.0"

resolvers ++= Seq(
  "iliaz.com" at "http://scala.iliaz.com/",
  "sonatype" at "http://oss.sonatype.org/content/repositories/releases",
  "sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "typesafe.com" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "6.0.4",
  "com.github.ornicar" %% "scalalib" % "3.3",
  // "play" %% "play" % "2.1.0" % "provided",
  "joda-time" % "joda-time" % "2.1",
  "org.joda" % "joda-convert" % "1.2",
  "org.reactivemongo" %% "reactivemongo" % "0.9-SNAPSHOT"
)
