package lila.challenge

import play.api.data.*
import play.api.data.Forms.*

import java.net.URLEncoder
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.libs.json.JsValue

import reactivemongo.api.bson.*

case class ChallengePref(
    variant: String,
    timeMode: String,
    gameMode: String,
    time: Int,
    increment: Int,
    days: Int,
    fen: String
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

  def unapply(o: ChallengePref): Option[(String, String, String, Int, Int, Int, String)] = Some(
    (o.variant, o.timeMode, o.gameMode, o.time, o.increment, o.days, o.fen)
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
    "variant"   -> text,
    "timeMode"  -> text,
    "gameMode"  -> text,
    "time"      -> number,
    "increment" -> number,
    "days"      -> number,
    "fen"       -> text
  )(ChallengePref.apply)(ChallengePref.unapply)
)

given BSONDocumentReader[ChallengePref] = Macros.reader
given BSONDocumentWriter[ChallengePref] = Macros.writer
