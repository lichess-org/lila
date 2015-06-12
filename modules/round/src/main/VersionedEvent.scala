package lila.round

import play.api.libs.json._

import actorApi.Member
import chess.Color
import lila.game.Event

case class VersionedEvent(
    version: Int,
    typ: String,
    data: JsValue,
    only: Option[Color],
    owner: Boolean,
    watcher: Boolean,
    troll: Boolean) {

  def jsFor(m: Member): JsObject = if (visibleBy(m)) {
    if (data == JsNull) Json.obj("v" -> version, "t" -> typ)
    else Json.obj("v" -> version, "t" -> typ, "d" -> data)
  }
  else Json.obj("v" -> version)

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
    data = e.data,
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
      data = r.strO("d").fold[JsValue](JsNull)(Json.parse),
      only = r boolO "o" map Color.apply,
      owner = r boolD "ow",
      watcher = r boolD "w",
      troll = r boolD "r")
    def writes(w: BSON.Writer, o: VersionedEvent) = BSONDocument(
      "v" -> o.version,
      "t" -> o.typ,
      "d" -> (o.data != JsNull).option(Json stringify o.data),
      "o" -> o.only.map(_.white),
      "ow" -> w.boolO(o.owner),
      "w" -> w.boolO(o.watcher),
      "t" -> w.boolO(o.troll))
  }
}
