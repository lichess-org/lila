package lila
package round

import play.api.libs.json._
import scalaz.effects._

import chess.Color
import memo.Builder

case class VersionedEvent(
    version: Int,
    typ: String,
    data: JsValue,
    only: Option[Color],
    owner: Boolean,
    watcher: Boolean) {

  def jsFor(m: Member): JsObject = visibleBy(m).fold(
    JsObject(Seq(
      "v" -> JsNumber(version),
      "t" -> JsString(typ),
      "d" -> data
    )),
    JsObject(Seq(
      "v" -> JsNumber(version)
    ))
  )

  private def visibleBy(m: Member): Boolean =
    if (watcher && m.owner) false
    else if (owner && m.watcher) false 
    else only.fold(true)(_ == m.color)
}

final class History(timeout: Int) {

  private var privateVersion = 0
  private val events = memo.Builder.expiry[Int, VersionedEvent](timeout)

  def version = privateVersion

  // none if version asked is > to history version
  // none if an event is missing (asked too old version)
  def since(v: Int): Option[List[VersionedEvent]] = version |> { u ⇒
    if (v > u) None
    else if (v == u) Some(Nil)
    else ((v + 1 to u).toList map event).sequence
  }

  private def event(v: Int) = Option(events getIfPresent v)

  def +=(event: Event): VersionedEvent = {
    privateVersion = version + 1
    val vevent = VersionedEvent(
      version = version,
      typ = event.typ,
      data = event.data,
      only = event.only,
      owner = event.owner,
      watcher = event.watcher)
    events.put(version, vevent)
    vevent
  }
}
