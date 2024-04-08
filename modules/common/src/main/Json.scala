package lila.common

import play.api.libs.json.{ Json as PlayJson, * }

import scala.util.NotGiven

object Json:

  export scalalib.json.Json.{ *, given }
  export chess.json.Json.given

  given userStrReads: Reads[UserStr] = Reads
    .of[String]
    .flatMapResult: str =>
      JsResult.fromTry(UserStr.read(str).toTry(s"Invalid username: $str"))

  given Writes[lila.core.relation.Relation] = writeAs(_.isFollow)

  given Reads[LilaOpeningFamily] = Reads[LilaOpeningFamily]: f =>
    f.get[String]("key")
      .flatMap(LilaOpeningFamily.find)
      .fold[JsResult[LilaOpeningFamily]](JsError(Nil))(JsSuccess(_))

  given NoJsonHandler[chess.Square] with {}

  import lila.core.LightUser
  given lightUserWrites: OWrites[LightUser] = OWrites(lightUser.write)
  object lightUser:
    def write(u: LightUser): JsObject = writeNoId(u) + ("id" -> JsString(u.id.value))
    def writeNoId(u: LightUser): JsObject = PlayJson
      .obj("name" -> u.name)
      .add("title", u.title)
      .add("flair", u.flair)
      .add("patron", u.isPatron)
