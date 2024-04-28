package lila.user

import lila.db.dsl.*
import lila.user.BSONFields as F
import lila.core.security.HashedPassword

case class AuthData(
    _id: UserId,
    bpass: HashedPassword,
    salt: Option[String] = None,
    sha512: Option[Boolean] = None
)

object AuthData:
  val projection = $doc(F.bpass -> true, F.salt -> true, F.sha512 -> true)
