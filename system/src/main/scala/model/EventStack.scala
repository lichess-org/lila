package lila.system
package model

import lila.chess._

case class EventStack(events: Seq[(Int, Event)]) {

  def encode: String = (events map {
    case (version, event) ⇒ event.encode map (version.toString + _)
  }).flatten mkString "|"

  // Here I found the mutable approach easier
  // I'm probably just missing something.
  def optimize: EventStack = {
    var previous: Boolean = false
    EventStack(
      (events.reverse take EventStack.maxEvents map {
        case (v, PossibleMovesEvent(_)) if previous ⇒ (v, PossibleMovesEvent(Map.empty))
        case (v, e @ PossibleMovesEvent(_))         ⇒ previous = true; (v, e)
        case x                                      ⇒ x
      }).reverse
    )
  }

  def version: Int = events.lastOption map (_._1) getOrElse 0

  def withEvents(newEvents: List[Event]): EventStack = {

    def versionEvents(v: Int, events: List[Event]): List[(Int, Event)] = events match {
      case Nil           ⇒ Nil
      case event :: rest ⇒ (v + 1, event) :: versionEvents(v + 1, rest)
    }

    copy(events = events ++ versionEvents(version, newEvents))
  }
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
    }).toSeq.flatten
  )

  def apply(): EventStack = new EventStack(Seq.empty)

  def build(events: Event*): EventStack =
    new EventStack(events.zipWithIndex map (_.swap) toSeq)
}
