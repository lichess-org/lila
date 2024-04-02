package lila.common

import chess.format.Uci
import chess.variant.Crazyhouse
import scalalib.Render
import play.api.libs.json.{ Json as PlayJson, * }

import scala.util.NotGiven

object Json:

  export scalalib.json.Json.{ *, given }

  given userStrReads: Reads[UserStr] = Reads
    .of[String]
    .flatMapResult: str =>
      JsResult.fromTry(UserStr.read(str).toTry(s"Invalid username: $str"))

  given Writes[chess.Color] = writeAs(_.name)

  given Reads[Uci] = Reads
    .of[String]
    .flatMapResult: str =>
      JsResult.fromTry(Uci(str).toTry(s"Invalid UCI: $str"))
  given Writes[Uci] = writeAs(_.uci)

  given Reads[LilaOpeningFamily] = Reads[LilaOpeningFamily]: f =>
    f.get[String]("key")
      .flatMap(LilaOpeningFamily.find)
      .fold[JsResult[LilaOpeningFamily]](JsError(Nil))(JsSuccess(_))

  given NoJsonHandler[chess.Square] with {}

  given OWrites[Crazyhouse.Pocket] = OWrites: p =>
    JsObject:
      p.flatMap((role, nb) => Option.when(nb > 0)(role.name -> JsNumber(nb)))

  given OWrites[chess.variant.Crazyhouse.Data] = OWrites: v =>
    PlayJson.obj("pockets" -> v.pockets.all)

  import lila.core.LightUser
  given lightUserWrites: OWrites[LightUser] = OWrites(lightUser.write)
  object lightUser:
    def write(u: LightUser): JsObject = writeNoId(u) + ("id" -> JsString(u.id.value))
    def writeNoId(u: LightUser): JsObject = PlayJson
      .obj("name" -> u.name)
      .add("title", u.title)
      .add("flair", u.flair)
      .add("patron", u.isPatron)
