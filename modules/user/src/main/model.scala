package lila.user

import lila.core.user.MyId
import lila.core.LightUser

final class GetBotIds(f: () => Fu[Set[UserId]]) extends (() => Fu[Set[UserId]]):
  def apply() = f()

final class RankingsOf(f: UserId => lila.rating.UserRankMap) extends (UserId => lila.rating.UserRankMap):
  def apply(u: UserId) = f(u)

/* User who is currently logged in */
opaque type Me = User
object Me extends TotalWrapper[Me, User]:
  export lila.core.user.MyId as Id
  given UserIdOf[Me]                           = _.id
  given Conversion[Me, User]                   = identity
  given Conversion[Me, UserId]                 = _.id
  given Conversion[Option[Me], Option[UserId]] = _.map(_.id)
  given [M[_]]: Conversion[M[Me], M[User]]     = Me.raw(_)
  given (using me: Me): Option[Me]             = Some(me)
  given lila.db.NoDbHandler[Me] with {}
  extension (me: Me)
    def userId: UserId        = me.id
    def lightMe: LightUser.Me = LightUser.Me(me.value.light)
    inline def modId: ModId   = userId.into(ModId)
    inline def myId: MyId     = userId.into(MyId)

given Conversion[Me, MyId]           = _.id.into(MyId)
given (using me: Me): MyId           = Me.myId(me)
given (using me: MyId): Option[MyId] = Some(me)
extension (me: Me.Id) inline def modId: ModId = me.into(ModId)
given (using me: Me): LightUser.Me = LightUser.Me(me.light)
