package lila.user

final class GetBotIds(f: () => Fu[Set[UserId]]) extends (() => Fu[Set[UserId]]):
  def apply() = f()

final class RankingsOf(f: UserId => lila.rating.UserRankMap) extends (UserId => lila.rating.UserRankMap):
  def apply(u: UserId) = f(u)

/* User who is currently logged in */
opaque type Me = User
object Me extends TotalWrapper[Me, User]:
  export lila.user.MyId as Id
  given UserIdOf[Me]                           = _.id
  given Conversion[Me, User]                   = identity
  given Conversion[Me, UserId]                 = _.id
  given Conversion[Option[Me], Option[UserId]] = _.map(_.id)
  given [M[_]]: Conversion[M[Me], M[User]]     = Me.raw(_)
  given (using me: Me): Option[Me]             = Some(me)
  given lila.db.NoDbHandler[Me] with {}
  extension (me: Me)
    def userId: UserId      = me.id
    inline def modId: ModId = userId into ModId
    inline def myId: MyId   = userId into MyId

opaque type MyId = String
object MyId extends TotalWrapper[MyId, String]:
  given UserIdOf[MyId]                         = u => u
  given Conversion[MyId, UserId]               = UserId(_)
  given [M[_]]: Conversion[M[MyId], M[UserId]] = u => UserId.from(MyId.raw(u))
  given Conversion[Me, MyId]                   = _.id into MyId
  given (using me: Me): MyId                   = Me.myId(me)
  given (using me: MyId): Option[MyId]         = Some(me)
  extension (me: Me.Id)
    inline def modId: ModId   = me into ModId
    inline def userId: UserId = me into UserId

opaque type UserEnabled = Boolean
object UserEnabled extends YesNo[UserEnabled]
