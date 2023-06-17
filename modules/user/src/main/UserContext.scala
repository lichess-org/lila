package lila.user

final class UserContext(val me: Option[User], val impersonatedBy: Option[User]):

  export me.{ isDefined as isAuth, isEmpty as isAnon }

  def is[U: UserIdOf](u: U): Boolean = me.exists(_ is u)

  def userId = me.map(_.id)

  def username = me.map(_.username)

  def usernameOrAnon = username | "Anonymous"

  def troll = me.exists(_.marks.troll)

  def kid   = me.exists(_.kid)
  def noKid = !kid

object UserContext:
  val anon = new UserContext(none, none)
