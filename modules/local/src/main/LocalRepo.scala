package lila.local

import reactivemongo.api.Cursor
import reactivemongo.api.bson.*
import lila.common.Json.opaqueFormat
import lila.db.JSON
import play.api.libs.json.*

import lila.db.dsl.{ *, given }

final private class LocalRepo(private[local] val bots: Coll, private[local] val assets: Coll)(using Executor):
  // given botMetaHandler: BSONDocumentHandler[BotMeta] = Macros.handler
  given Format[BotMeta] = Json.format

  def getVersions(botId: Option[UserId] = none): Fu[JsArray] =
    bots
      .find(botId.fold[Bdoc]($doc())(v => $doc("uid" -> v)), $doc("_id" -> 0).some)
      .sort($doc("version" -> -1))
      .cursor[Bdoc]()
      .list(Int.MaxValue)
      .map: docs =>
        JsArray(docs.map(JSON.jval))

  def getLatestBots(): Fu[JsArray] =
    bots
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Sort(Descending("version")),
          GroupField("uid")("doc" -> FirstField("$ROOT")),
          ReplaceRootField("doc"),
          Project($doc("_id" -> 0))
        )
      .list(Int.MaxValue)
      .map: docs =>
        JsArray(docs.flatMap(JSON.jval(_).asOpt[JsObject]))

  def putBot(bot: JsObject, author: UserId): Fu[JsObject] =
    val botId = (bot \ "uid").as[UserId]
    for
      nextVersion <- bots
        .find($doc("uid" -> botId))
        .sort($doc("version" -> -1))
        .one[Bdoc]
        .map(_.flatMap(_.getAsOpt[Int]("version")).getOrElse(-1) + 1) // race condition
      botMeta = BotMeta(botId, author, nextVersion)
      newBot  = bot ++ Json.toJson(botMeta).as[JsObject]
      _ <- bots.insert.one(JSON.bdoc(newBot))
    yield newBot

  def getAssets: Fu[Map[String, String]] =
    assets
      .find($doc())
      .cursor[Bdoc]()
      .list(Int.MaxValue)
      .map { docs =>
        docs.flatMap { doc =>
          for
            id   <- doc.getAsOpt[String]("_id")
            name <- doc.getAsOpt[String]("name")
          yield id -> name
        }.toMap
      }

  def nameAsset(tpe: Option[AssetType], key: String, name: String, author: Option[String]): Funit =
    // filter out bookCovers as they share the same key as the book
    if !tpe.has("book") || !key.endsWith(".png") then
      val id     = if tpe.has("book") then key.dropRight(4) else key
      val setDoc = author.fold($doc("name" -> name))(a => $doc("name" -> name, "author" -> a))
      assets.update.one($doc("_id" -> id), $doc("$set" -> setDoc), upsert = true).void
    else funit

  def deleteAsset(key: String): Funit =
    assets.delete.one($doc("_id" -> key)).void

end LocalRepo
