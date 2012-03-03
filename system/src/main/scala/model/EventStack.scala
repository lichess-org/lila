package lila.system
package model

import lila.chess._
import Piotr._

case class EventStack(events: IndexedSeq[(Int, Event)]) {

  def encode: String = (events map {
    case (version, event) ⇒ event.encode map (version.toString + _)
  }).flatten mkString "|"

  // Here I found the mutable approach easier
  // I'm probably just missing something.
  def optimize: EventStack = {
    var previous: Boolean = false
    EventStack(
      (events.toList.reverse take EventStack.maxEvents map {
        case (v, PossibleMovesEvent(_)) if previous ⇒ (v, PossibleMovesEvent(Map.empty))
        case (v, e @ PossibleMovesEvent(_)) ⇒ previous = true; (v, e)
        case x ⇒ x
      }).reverse.toIndexedSeq
    )
  }

  def withMove(move: Move): EventStack = this
}

object EventStack {

  val maxEvents = 16

  val EventEncoding = """^(\d+)(\w)(.*)$""".r

  def decode(evts: String): EventStack = new EventStack(
    (evts.split('|') collect {
      case EventEncoding(v, code, data) ⇒ for {
        version ← parseIntOption(v)
        decoder ← EventDecoder.all get code(0)
        event ← decoder decode data
      } yield (version, event)
    }).toIndexedSeq.flatten
  )

  def apply(events: Event*): EventStack =
    new EventStack(events.zipWithIndex map (_.swap) toIndexedSeq)
}
