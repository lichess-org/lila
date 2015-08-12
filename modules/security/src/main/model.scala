package lila.security

case class FingerprintedUser(user: lila.user.User, hasFingerprint: Boolean)
