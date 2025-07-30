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
    patronMonths: Int // 0 if no plan is ongoing
):
  def titleName: String = title.fold(name.value)(_.value + " " + name)
  def isBot = title.contains(PlayerTitle.BOT)
  def isPatron: Boolean = patronMonths > 0

object LightUser:

  type Ghost = LightUser

  val ghost: Ghost = LightUser(UserId("ghost"), UserName("ghost"), None, None, 0)

  given UserIdOf[LightUser] = _.id

  def fallback(name: UserName) = LightUser(
    id = name.id,
    name = name,
    title = None,
    flair = None,
    patronMonths = 0
  )

  def patronTier(patronMonths: Int): Option[String] = patronMonths match
    case m if m >= 60 => "5years".some
    case m if m >= 48 => "4years".some
    case m if m >= 36 => "3years".some
    case m if m >= 24 => "2years".some
    case m if m >= 12 => "1year".some
    case m if m >= 9 => "9months".some
    case m if m >= 6 => "6months".some
    case m if m >= 3 => "3months".some
    case m if m >= 2 => "2months".some
    case m if m >= 1 => "1month".some
    case _ => None

  opaque type Me = LightUser
  object Me extends TotalWrapper[Me, LightUser]:
    extension (me: Me) def userId: UserId = me.id
    given UserIdOf[Me] = _.id
    given Conversion[Me, LightUser] = identity
    given (using me: Me): MyId = me.id.into(MyId)
    given (using me: lila.core.user.Me): Me = me.lightMe

  private type GetterType = UserId => Fu[Option[LightUser]]
  opaque type Getter <: GetterType = GetterType
  object Getter extends TotalWrapper[Getter, GetterType]

  private type GetterFallbackType = UserId => Fu[LightUser]
  opaque type GetterFallback <: GetterFallbackType = GetterFallbackType
  object GetterFallback extends TotalWrapper[GetterFallback, GetterFallbackType]:
    extension (e: GetterFallback)
      def optional = Getter(id => e(id).map(Some(_))(using ExecutionContext.parasitic))

  private type GetterSyncType = UserId => Option[LightUser]
  opaque type GetterSync <: GetterSyncType = GetterSyncType
  object GetterSync extends TotalWrapper[GetterSync, GetterSyncType]

  private type GetterSyncFallbackType = UserId => LightUser
  opaque type GetterSyncFallback <: GetterSyncFallbackType = GetterSyncFallbackType
  object GetterSyncFallback extends TotalWrapper[GetterSyncFallback, GetterSyncFallbackType]

  private type IsBotSyncType = UserId => Boolean
  opaque type IsBotSync <: IsBotSyncType = IsBotSyncType
  object IsBotSync extends TotalWrapper[IsBotSync, IsBotSyncType]
