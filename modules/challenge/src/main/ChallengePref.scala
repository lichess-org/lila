package lila.challenge

import play.api.data.*
import play.api.data.Forms.*

import java.net.URLEncoder
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.libs.json.JsValue

import reactivemongo.api.bson.*

case class ChallengePref(
    gameType: String,
    variant: String,
    timeMode: String,
    gameMode: String,
    time: Int,
    increment: Int,
    days: Int,
    fen: String,
    color: String,
    ratingMin: Int,
    ratingMax: Int,
    aiLevel: Int,
)

object ChallengePref:
  implicit val challengePrefWrites: Writes[ChallengePref] = new Writes[ChallengePref]:
    def writes(o: ChallengePref): JsValue = Json.obj(
      "variant"   -> o.variant,
      "timeMode"  -> o.timeMode,
      "gameMode"  -> o.gameMode,
      "time"      -> o.time,
      "increment" -> o.increment,
      "days"      -> o.days,
      "fen"       -> o.fen
    )

  def unapply(o: ChallengePref): Option[(String, String, String, String, Int, Int, Int, String, String, Int, Int, Int)] = Some(
    (o.gameType, o.variant, o.timeMode, o.gameMode, o.time, o.increment, o.days, o.fen, o.color, o.ratingMin, o.ratingMax, o.aiLevel)
  )

  def asEncodedUrlAttr(pref: Option[ChallengePref]): String =
    pref match
      case Some(obj) =>
        val jsonString = Json.stringify(Json.toJson(obj))
        val encoded    = URLEncoder.encode(jsonString, "UTF-8")
        s"&challenge-pref=${encoded}"
      case None => ""

val challengePref = Form(
  mapping(
    "gameType"  -> text,
    "variant"   -> text,
    "timeMode"  -> text,
    "gameMode"  -> text,
    "time"      -> number,
    "increment" -> number,
    "days"      -> number,
    "fen"       -> text,
    "color"     -> text,
    "ratingMin" -> number,
    "ratingMax" -> number,
    "aiLevel" -> number,
  )(ChallengePref.apply)(ChallengePref.unapply)
)

given BSONDocumentReader[ChallengePref] = Macros.reader
given BSONDocumentWriter[ChallengePref] = Macros.writer
