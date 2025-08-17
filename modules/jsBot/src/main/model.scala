package lila.jsBot

import play.api.libs.json.*

import lila.common.Json.{ *, given }

opaque type BotJson = JsObject
object BotJson extends OpaqueJson[BotJson]:
  extension (b: BotJson)
    def uid = (b \ "uid").as[UserId]
    def withMeta(meta: BotMeta): BotJson = b ++ Json.toJsObject(meta)

case class BotMeta(uid: UserId, author: UserId, version: Int)
private given OWrites[BotMeta] = Json.writes

type AssetType = "sound" | "image" | "book" | "net"
type AssetKey = String
type AssetName = String

object AssetType:
  def read(s: String): Option[AssetType] = s match
    case at: AssetType => Some(at)
    case _ => None
