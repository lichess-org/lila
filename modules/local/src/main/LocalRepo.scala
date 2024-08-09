package lila.local

import reactivemongo.api.Cursor
import reactivemongo.api.bson.*
import lila.common.Json.opaqueFormat
import lila.db.JSON
import play.api.libs.json.*

import lila.db.dsl.{ *, given }

final private class LocalRepo(private[local] val coll: Coll)(using Executor):
  given handler: BSONDocumentHandler[BotMeta] = Macros.handler
  given Format[BotMeta]                       = Json.format

  def getAll(): Fu[JsArray] =
    coll
      .find($doc(), $doc("_id" -> 0).some)
      .sort($doc("version" -> -1))
      .cursor[Bdoc]()
      .list(Int.MaxValue)
      .map: docs =>
        JsArray(docs.map(JSON.jval))

  def getLatest(): Fu[JsArray] =
    coll
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
//
  def put(bot: JsObject, author: UserId): Fu[JsObject] =
    val botId = (bot \ "uid").as[UserId]
    for
      nextVersion <- coll
        .find($doc("uid" -> botId))
        .sort($doc("version" -> -1))
        .one[Bdoc]
        .map(_.flatMap(_.getAsOpt[Int]("version")).getOrElse(-1) + 1) // race condition
      botMeta = BotMeta(botId, author, nextVersion)
      newBot  = bot ++ Json.toJson(botMeta).as[JsObject]
      _ <- coll.insert.one(JSON.bdoc(newBot))
    yield newBot
end LocalRepo
