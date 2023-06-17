package lila.user

final class GetBotIds(f: () => Fu[Set[UserId]]) extends (() => Fu[Set[UserId]]):
  def apply() = f()

final class RankingsOf(f: UserId => lila.rating.UserRankMap) extends (UserId => lila.rating.UserRankMap):
  def apply(u: UserId) = f(u)

// permission holder
case class Holder(user: User) extends AnyVal:
  def id = user.id

/* User who is currently logged in */
opaque type Me = User
object Me extends TotalWrapper[Me, User]:
  given UserIdOf[Me]                       = _.id
  given Conversion[Me, User]               = identity
  given [M[_]]: Conversion[M[Me], M[User]] = Me.raw(_)
  extension (me: Me)
    inline def userId: UserId = me.id
    inline def user: User     = me
    // inline def is[U](u: U)(using idOf: UserIdOf[U]): Boolean = me.id == idOf(u)

opaque type UserEnabled = Boolean
object UserEnabled extends YesNo[UserEnabled]
