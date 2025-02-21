package lila.local

case class BotMeta(uid: UserId, author: UserId, version: Int)

type AssetType = "sound" | "image" | "book" | "net"
type AssetKey  = String
type AssetName = String

def assetTypeOpt(s: String): Option[AssetType] = s match
  case at: AssetType => Some(at)
  case _             => None
