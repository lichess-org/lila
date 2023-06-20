package lila.user

final class UserContext(val me: Option[Me], val impersonatedBy: Option[User]):

  export me.{ isDefined as isAuth, isEmpty as isAnon }

  def is[U: UserIdOf](u: U): Boolean = me.exists(_ is u)

  inline def user: Option[User] = Me raw me

  def meId: Option[MyId]         = me.map(_.meId)
  def userId: Option[UserId]     = user.map(_.id)
  def username: Option[UserName] = user.map(_.username)
  def usernameOrAnon: String     = username.fold("Anonymous")(_.value)

  def troll: Boolean = user.exists(_.marks.troll)
  def kid: Boolean   = user.exists(_.kid)
  def noKid: Boolean = !kid

object UserContext:
  val anon = UserContext(none, none)
