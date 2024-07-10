package lila.common

import play.api.libs.json.{ Json as PlayJson, * }
import io.mola.galimatias.URL

object Json:

  export scalalib.json.Json.{ *, given }
  export chess.json.Json.given

  given userStrReads: Reads[UserStr] = Reads
    .of[String]
    .flatMapResult: str =>
      JsResult.fromTry(UserStr.read(str).toTry(s"Invalid username: $str"))

  given Writes[lila.core.relation.Relation] = writeAs(_.isFollow)

  given Writes[PerfKey] = pk => JsString(PerfKey.value(pk))

  given Writes[URL] = url => JsString(url.toString)

  given Writes[chess.PlayerTitle] = tile => JsString(tile.value)

  given NoJsonHandler[chess.Square] with {}

  // could be in scalalib
  given [A](using sr: SameRuntime[A, String]): KeyWrites[A] with
    def writeKey(key: A) = sr(key)

  import lila.core.LightUser
  given lightUserWrites: OWrites[LightUser] = OWrites(lightUser.write)
  object lightUser:
    def write(u: LightUser): JsObject = writeNoId(u) + ("id" -> JsString(u.id.value))
    def writeNoId(u: LightUser): JsObject = PlayJson
      .obj("name" -> u.name)
      .add("title", u.title)
      .add("flair", u.flair)
      .add("patron", u.isPatron)
