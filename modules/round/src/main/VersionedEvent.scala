package lila.round

import play.api.libs.functional.syntax._
import play.api.libs.json._

import actorApi.Member
import chess.Color
import lila.common.ApiVersion
import lila.game.Event
import org.apache.commons.lang3.StringEscapeUtils.escapeHtml4

case class VersionedEvent(
    version: Int,
    typ: String,
    encoded: Either[String, JsValue],
    only: Option[Color],
    owner: Boolean,
    watcher: Boolean,
    troll: Boolean) {

  lazy val decoded: JsValue = encoded match {
    case Left(s)   => Json parse s
    case Right(js) => js
  }

  def jsFor(m: Member): JsObject = if (visibleBy(m)) {
    if (decoded == JsNull) Json.obj("v" -> version, "t" -> typ)
    else Json.obj(
      "v" -> version,
      "t" -> typ,
      "d" -> dataForApiVersion(typ, decoded, m.apiVersion))
  }
  else Json.obj("v" -> version)

  private val mobileV1Escaper: Reads[JsObject] = (__ \ 't).json.update(
    __.read[JsString].map { s => JsString(escapeHtml4(s.value)) }
  )

  private def dataForApiVersion(typ: String, data: JsValue, apiVersion: ApiVersion): JsValue =
    if (typ == "message" && apiVersion.v1) data match {
      case o: JsObject => o transform mobileV1Escaper getOrElse o
      case v           => v
    }
    else data

  private def visibleBy(m: Member): Boolean =
    if (watcher && m.owner) false
    else if (owner && m.watcher) false
    else if (troll && !m.troll) false
    else only.fold(true)(_ == m.color)

  override def toString = s"Event $version $typ"
}

private[round] object VersionedEvent {

  def apply(e: Event, v: Int): VersionedEvent = VersionedEvent(
    version = v,
    typ = e.typ,
    encoded = Right(e.data),
    only = e.only,
    owner = e.owner,
    watcher = e.watcher,
    troll = e.troll)

  import lila.db.BSON
  import reactivemongo.bson._

  implicit val versionedEventHandler = new BSON[VersionedEvent] {
    def reads(r: BSON.Reader) = VersionedEvent(
      version = r int "v",
      typ = r str "t",
      encoded = r.strO("d").map(Left.apply).getOrElse(Right(JsNull)),
      only = r boolO "o" map Color.apply,
      owner = r boolD "ow",
      watcher = r boolD "w",
      troll = r boolD "r")
    def writes(w: BSON.Writer, o: VersionedEvent) = BSONDocument(
      "v" -> o.version,
      "t" -> o.typ,
      "d" -> (o.encoded match {
        case Left(s)       => s.some
        case Right(JsNull) => none
        case Right(js)     => Json.stringify(js).some
      }),
      "o" -> o.only.map(_.white),
      "ow" -> w.boolO(o.owner),
      "w" -> w.boolO(o.watcher),
      "r" -> w.boolO(o.troll))
  }
}
