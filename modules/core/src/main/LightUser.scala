package lila.core

import _root_.chess.PlayerTitle

import scala.concurrent.ExecutionContext

import lila.core.id.Flair
import lila.core.userId.*

case class LightUser(
    id: UserId,
    name: UserName,
    title: Option[PlayerTitle],
    flair: Option[Flair],
    isPatron: Boolean
):
  def titleName: String = title.fold(name.value)(_.value + " " + name)
  def isBot             = title.contains(PlayerTitle.BOT)

object LightUser:

  type Ghost = LightUser

  val ghost: Ghost = LightUser(UserId("ghost"), UserName("ghost"), None, None, false)

  given UserIdOf[LightUser] = _.id

  def fallback(name: UserName) = LightUser(
    id = name.id,
    name = name,
    title = None,
    flair = None,
    isPatron = false
  )

  opaque type Me = LightUser
  object Me extends TotalWrapper[Me, LightUser]:
    extension (me: Me) def userId: UserId   = me.id
    given UserIdOf[Me]                      = _.id
    given Conversion[Me, LightUser]         = identity
    given (using me: Me): MyId              = me.id.into(MyId)
    given (using me: lila.core.user.Me): Me = me.lightMe

  private type GetterType          = UserId => Fu[Option[LightUser]]
  opaque type Getter <: GetterType = GetterType
  object Getter extends TotalWrapper[Getter, GetterType]

  private type GetterFallbackType                  = UserId => Fu[LightUser]
  opaque type GetterFallback <: GetterFallbackType = GetterFallbackType
  object GetterFallback extends TotalWrapper[GetterFallback, GetterFallbackType]:
    extension (e: GetterFallback)
      def optional = Getter(id => e(id).map(Some(_))(using ExecutionContext.parasitic))

  private type GetterSyncType              = UserId => Option[LightUser]
  opaque type GetterSync <: GetterSyncType = GetterSyncType
  object GetterSync extends TotalWrapper[GetterSync, GetterSyncType]

  private type GetterSyncFallbackType                      = UserId => LightUser
  opaque type GetterSyncFallback <: GetterSyncFallbackType = GetterSyncFallbackType
  object GetterSyncFallback extends TotalWrapper[GetterSyncFallback, GetterSyncFallbackType]

  private type IsBotSyncType             = UserId => Boolean
  opaque type IsBotSync <: IsBotSyncType = IsBotSyncType
  object IsBotSync extends TotalWrapper[IsBotSync, IsBotSyncType]
