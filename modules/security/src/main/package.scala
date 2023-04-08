package lila.security

export lila.Lila.{ *, given }

private val logger = lila.log("security")

lazy val userAgentParser = org.uaparser.scala.Parser.default
