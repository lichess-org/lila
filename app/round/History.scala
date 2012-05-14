package lila
package round

import play.api.libs.json._
import scalaz.effects._

import chess.Color
import memo.Builder

final class History(timeout: Int) {

  case class VersionedEvent(js: JsObject, only: Option[Color], own: Boolean) {

    def visible(color: Color, owner: Boolean): Boolean =
      if (own && !owner) false else only.fold(_ == color, true)

    def visible(member: Member): Boolean = visible(member.color, member.owner)
  }

  private var privateVersion = 0
  private val events = memo.Builder.expiry[Int, VersionedEvent](timeout)

  def version = privateVersion

  def since(v: Int): List[VersionedEvent] =
    (v + 1 to version).toList map event flatten

  private def event(v: Int) = Option(events getIfPresent v)

  def +=(event: Event): VersionedEvent = {
    privateVersion = privateVersion + 1
    val vevent = VersionedEvent(
      js = JsObject(Seq(
        "v" -> JsNumber(privateVersion),
        "t" -> JsString(event.typ),
        "d" -> event.data
      )),
      only = event.only,
      own = event.owner)
    events.put(privateVersion, vevent)
    vevent
  }
}
