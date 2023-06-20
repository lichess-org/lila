package lila.user

final class UserContext(val me: Option[Me], val needsFp: Boolean, val impersonatedBy: Option[User]):

  def usernameOrAnon: String = me.map(_.username).fold("Anonymous")(_.value)

object UserContext:
  val anon = UserContext(none, false, none)
