package lila.local

import reactivemongo.api.Cursor
import reactivemongo.api.bson.*
import lila.common.Json.opaqueFormat

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
        JsArray(docs.map(bdocToJsVal))

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
        JsArray(docs.flatMap(bdocToJsVal(_).asOpt[JsObject]))

  def put(bot: JsObject, author: UserId): Fu[JsObject] =
    val botId = (bot \ "uid").as[UserId]
    for
      highestVersion <- coll
        .find($doc("uid" -> botId))
        .sort($doc("version" -> -1))
        .one[Bdoc]
        .map(_.flatMap(_.getAsOpt[Int]("version")).getOrElse(0))
      nextVersion = highestVersion + 1
      botMeta     = BotMeta(botId, author, nextVersion)
      newBot      = bot ++ Json.toJson(botMeta).as[JsObject]
      _ <- coll.insert.one(jsObjToBdoc(newBot))
    yield newBot

  private def jsValToBval(json: JsValue): BSONValue = json match
    case JsString(value)  => BSONString(value)
    case JsNumber(value)  => BSONDouble(value.toDouble)
    case JsBoolean(value) => BSONBoolean(value)
    case obj: JsObject    => jsObjToBdoc(obj)
    case JsArray(arr)     => BSONArray(arr.map(jsValToBval))
    case _                => BSONNull

  private def bvalToJsVal(bson: BSONValue): JsValue = bson match
    case BSONString(value)  => JsString(value)
    case BSONDouble(value)  => JsNumber(value)
    case BSONBoolean(value) => JsBoolean(value)
    case obj: Bdoc          => bdocToJsVal(obj)
    case BSONArray(values)  => JsArray(values.map(bvalToJsVal))
    case _                  => JsNull

  private def jsObjToBdoc(json: JsObject): Bdoc =
    BSONDocument(json.fields.map { case (k, v) => k -> jsValToBval(v) })

  private def bdocToJsVal(bson: Bdoc): JsValue = Json.obj(
    bson.elements.map(element => element.name -> bvalToJsVal(element.value))*
  )
end LocalRepo
