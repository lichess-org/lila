package lila.system
package model

import lila.chess._
import Piotr._

case class EventStack(events: IndexedSeq[(Int, Event)]) {

  def encode: String = (events map {
    case (version, event) ⇒ event.encode map (version.toString + _)
  }).flatten mkString "|"

  def withMove(move: Move): EventStack = this
}

object EventStack {

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
