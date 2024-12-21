package lila.local

import reactivemongo.api.bson.*
// import reactivemongo.api.bson.collection.BSONCollection
import play.api.libs.json.*

case class GameSetup(
    white: Option[String],
    black: Option[String],
    fen: Option[String],
    initial: Option[Float],
    increment: Option[Float],
    go: Boolean = false
)

case class BotMeta(uid: UserId, author: UserId, version: Int)

type AssetType = "sound" | "image" | "book"
