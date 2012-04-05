package lila
package model

import chess._

case class EventStack(events: List[(Int, Event)]) {

  lazy val sortedEvents = events sortBy (_._1)

  lazy val firstVersion: Int = sortedEvents.headOption map (_._1) getOrElse 0

  lazy val lastVersion: Int = sortedEvents.lastOption map (_._1) getOrElse 0

  def encode: String = events map {
    case (version, event) ⇒ version.toString + event.encode
  } mkString "|"

  // Here I found the mutable approach easier
  // I'm probably just missing something.
  // Like the state monad.
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

  def eventsSince(version: Int): Option[List[Event]] =
    if (version >= (firstVersion - 1) && version <= lastVersion)
      Some(sortedEvents dropWhile { ve ⇒ ve._1 <= version } map (_._2))
    else None

  def withEvents(newEvents: List[Event]): EventStack = {

    def versionEvents(v: Int, events: List[Event]): List[(Int, Event)] = events match {
      case Nil           ⇒ Nil
      case event :: rest ⇒ (v + 1, event) :: versionEvents(v + 1, rest)
    }

    copy(events = events ++ versionEvents(lastVersion, newEvents))
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
    }).toList.flatten
  )

  def apply(): EventStack = new EventStack(Nil)

  def build(events: Event*): EventStack =
    new EventStack(events.zipWithIndex map (_.swap) toList)
}
