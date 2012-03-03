package lila.system
package model

import lila.chess._
import Piotr._

case class EventStack(events: Map[Int, Event]) {

  def encode: String = (events map {
    case (version, event) ⇒ event.encode map (version.toString + _)
  }).flatten mkString "|"

  def withMove(move: Move): EventStack = this
}

object EventStack {

  val EventEncoding = """^(\d+)(\w)(.*)$""".r

  def decode(evts: String): EventStack = new EventStack(
    (evts.split('|').toList collect {
      case EventEncoding(version, code, data) ⇒ for {
        decoder ← EventDecoder.all get code(0)
        event ← decoder decode data
      } yield (version.toInt, event)
    }).flatten toMap
  )

  def apply(events: Event*): EventStack =
    new EventStack(events.zipWithIndex map (_.swap) toMap)
}
