resolvers += "lila-maven" at "https://raw.githubusercontent.com/ornicar/lila-maven/master"
addSbtPlugin("com.typesafe.play" % "sbt-plugin"   % "2.8.2-lila_0.1" from "https://raw.githubusercontent.com/ornicar/lila-maven/master/com.typesafe.play/scala_2.12/sbt_1.0/2.8.2-lila_0.1/jars/sbt-plugin.jar")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("ch.epfl.scala"     % "sbt-bloop"    % "1.4.3")
