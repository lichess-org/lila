package lila
package tournament

import play.api.libs.json._
import scalaz.effects._

import memo.Builder

case class VersionedEvent(
    version: Int,
    typ: String,
    data: JsValue,
    only: Option[String]) {

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
    only.fold(u ⇒ m.username.fold(u ==, false), true)
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
      only = event.only)
    events.put(version, vevent)
    vevent
  }
}
