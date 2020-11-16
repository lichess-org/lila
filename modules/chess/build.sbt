name := "scalachess"

organization := "org.lishogi"

version := "9.3.1"

scalaVersion := "2.13.2"

libraryDependencies ++= List(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "org.scalaz"             %% "scalaz-core"              % "7.2.30",
  "org.specs2"             %% "specs2-core"              % "4.7.0" % "test",
  "org.specs2"             %% "specs2-scalaz"            % "4.7.0" % "test",
  "com.github.ornicar"     %% "scalalib"                 % "6.8",
  "joda-time"              % "joda-time"                 % "2.10.6"
)

resolvers ++= Seq(
  "lila-maven" at "https://raw.githubusercontent.com/ornicar/lila-maven/master",
  "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases"
)

scalacOptions ++= Seq(
  "-language:implicitConversions",
  "-language:postfixOps",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xlint:_",
  "-Ywarn-macros:after",
  "-Ywarn-unused:_",
  "-Xmaxerrs",
  "12",
  "-Xmaxwarns",
  "12"
)

publishTo := Some(Resolver.file("file", new File(sys.props.getOrElse("publishTo", ""))))
