package lila.security

export lila.Lila.{ *, given }

private val logger = lila.log("security")

opaque type UserStrOrEmail = String
object UserStrOrEmail extends OpaqueString[UserStrOrEmail]

lazy val userAgentParser = org.uaparser.scala.Parser.default
