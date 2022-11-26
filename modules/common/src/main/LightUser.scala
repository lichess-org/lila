package lila.common

import play.api.libs.json.*
import lila.common.Json.given

case class LightUser(
    id: String,
    name: String,
    title: Option[UserTitle],
    isPatron: Boolean
):

  def titleName = title.fold(name)(_.value + " " + name)

  def isBot = title has "BOT"

  def is(name: String) = id == LightUser.normalize(name)

object LightUser:

  type Ghost          = LightUser
  private type UserID = String

  val ghost: Ghost = LightUser("ghost", "ghost", none, false)

  given lightUserWrites: OWrites[LightUser] = OWrites { u =>
    writeNoId(u) + ("id" -> JsString(u.id))
  }

  def writeNoId(u: LightUser): JsObject =
    Json
      .obj("name" -> u.name)
      .add("title" -> u.title)
      .add("patron" -> u.isPatron)

  def fallback(name: String) = LightUser(
    id = normalize(name),
    name = name,
    title = None,
    isPatron = false
  )

  def normalize(name: String) = name.toLowerCase

  private type GetterType          = UserID => Fu[Option[LightUser]]
  opaque type Getter <: GetterType = GetterType
  object Getter extends TotalWrapper[Getter, GetterType]

  private type GetterSyncType              = UserID => Option[LightUser]
  opaque type GetterSync <: GetterSyncType = GetterSyncType
  object GetterSync extends TotalWrapper[GetterSync, GetterSyncType]

  private type IsBotSyncType             = UserID => Boolean
  opaque type IsBotSync <: IsBotSyncType = IsBotSyncType
  object IsBotSync extends TotalWrapper[IsBotSync, IsBotSyncType]
