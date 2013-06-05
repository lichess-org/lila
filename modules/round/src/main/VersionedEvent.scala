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

  def jsFor(m: Member): JsObject = visibleBy(m).fold(
    Json.obj(
      "v" -> version,
      "t" -> typ,
      "d" -> data
    ),
    Json.obj("v" -> version)
  )

  private def visibleBy(m: Member): Boolean =
    if (watcher && m.owner) false
    else if (owner && m.watcher) false
    else if (troll && !m.troll) false
    else only.fold(true)(_ == m.color)
}
