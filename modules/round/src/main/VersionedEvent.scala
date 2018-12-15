package lila.round

import play.api.libs.json._

import actorApi.Member
import chess.Color
import lila.game.Event
import lila.socket.Socket.{ SocketVersion, socketVersionFormat }

case class VersionedEvent(
    version: SocketVersion,
    typ: String,
    encoded: Either[String, JsValue],
    only: Option[Color],
    owner: Boolean,
    watcher: Boolean,
    troll: Boolean
) {

  lazy val decoded: JsValue = encoded.fold(Json.parse, identity)

  def jsFor(m: Member): JsObject = if (visibleBy(m)) {
    if (decoded == JsNull) Json.obj("v" -> version, "t" -> typ)
    else Json.obj(
      "v" -> version,
      "t" -> typ,
      "d" -> decodedFor(typ, m)
    )
  } else Json.obj("v" -> version)

  private def decodedFor(typ: String, m: Member) =
    if ((typ == "move" || typ == "drop") && m.watcher) decoded.as[JsObject] - "dests"
    else decoded

  private def visibleBy(m: Member): Boolean =
    !(watcher && m.owner) &&
      !(owner && m.watcher) &&
      !(troll && !m.troll) &&
      only.fold(true)(_ == m.color)

  override def toString = s"Event $version $typ"
}

private[round] object VersionedEvent {

  def apply(e: Event, v: SocketVersion): VersionedEvent = VersionedEvent(
    version = v,
    typ = e.typ,
    encoded = Right(e.data),
    only = e.only,
    owner = e.owner,
    watcher = e.watcher,
    troll = e.troll
  )

  import lila.db.BSON
  import reactivemongo.bson._

  private implicit val SocketVersionHandler = lila.db.dsl.intAnyValHandler[SocketVersion](_.value, SocketVersion.apply)

  implicit val versionedEventHandler = new BSON[VersionedEvent] {
    def reads(r: BSON.Reader) = VersionedEvent(
      version = r.get[SocketVersion]("v"),
      typ = r str "t",
      encoded = r.strO("d").map(Left.apply).getOrElse(Right(JsNull)),
      only = r boolO "o" map Color.apply,
      owner = r boolD "ow",
      watcher = r boolD "w",
      troll = r boolD "r"
    )
    def writes(w: BSON.Writer, o: VersionedEvent) = BSONDocument(
      "v" -> o.version,
      "t" -> o.typ,
      "d" -> (o.encoded match {
        case Left(s) => s.some
        case Right(JsNull) => none
        case Right(js) => Json.stringify(js).some
      }),
      "o" -> o.only.map(_.white),
      "ow" -> w.boolO(o.owner),
      "w" -> w.boolO(o.watcher),
      "r" -> w.boolO(o.troll)
    )
  }
}
