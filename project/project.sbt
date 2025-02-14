// 2.12 compiler flags for meta build
scalacOptions ++= Seq(
  "-feature",
  "-language:postfixOps",
  "-unchecked",
  "-deprecation",
  "-Ywarn-dead-code",
)
