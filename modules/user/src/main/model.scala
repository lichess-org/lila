package lila.user

import alleycats.Zero

final class GetBotIds(f: () => Fu[Set[UserId]]) extends (() => Fu[Set[UserId]]):
  def apply() = f()

final class RankingsOf(f: UserId => lila.rating.UserRankMap) extends (UserId => lila.rating.UserRankMap):
  def apply(u: UserId) = f(u)

/* User who is currently logged in */
opaque type Me = User
object Me extends TotalWrapper[Me, User]:
  export lila.user.MeId as Id
  given UserIdOf[Me]                       = _.id
  given meToUser: Conversion[Me, User]     = identity
  given meToUserId: Conversion[Me, UserId] = _.id
  given [M[_]]: Conversion[M[Me], M[User]] = Me.raw(_)
  extension (me: Me)
    inline def userId: UserId = me.id
    inline def user: User     = me
    inline def modId: ModId   = userId into ModId
    inline def meId: MeId     = userId into MeId
    // inline def is[U](u: U)(using idOf: UserIdOf[U]): Boolean = me.id == idOf(u)

opaque type MeId = String
object MeId extends TotalWrapper[MeId, String]:
  given UserIdOf[MeId]           = u => u
  given Conversion[MeId, UserId] = identity
  given Conversion[Me, MeId]     = _.id into MeId
  given (using me: Me): MeId     = Me.meId(me)
  extension (me: Me.Id)
    inline def modId: ModId   = me into ModId
    inline def userId: UserId = me into UserId

opaque type UserEnabled = Boolean
object UserEnabled extends YesNo[UserEnabled]
