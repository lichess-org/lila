resolvers += "jitpack".at("https://jitpack.io")

addSbtPlugin("com.github.lichess-org.liplay" % "sbt-plugin" % "3.2.0")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.7")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")
addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "2.1.2")
