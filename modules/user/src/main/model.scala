package lila.user

final class GetBotIds(f: () => Fu[Set[UserId]]) extends (() => Fu[Set[UserId]]):
  def apply() = f()

final class RankingsOf(f: UserId => lila.rating.UserRankMap) extends (UserId => lila.rating.UserRankMap):
  def apply(u: UserId) = f(u)

// permission holder
case class Holder(user: User) extends AnyVal:
  def id = user.id

opaque type Me = User
object Me extends TotalWrapper[Me, User]:
  given UserIdOf[Me]                = _.id
  given Conversion[Me, User]        = identity
  extension (e: Me) inline def user = e

opaque type UserEnabled = Boolean
object UserEnabled extends YesNo[UserEnabled]
