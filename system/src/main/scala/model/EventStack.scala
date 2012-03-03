package lila.system
package model

import lila.chess.Move

case class EventStack(events: List[Event]) {

  def encode: String = ""

  def withMove(move: Move): EventStack = this
}

object EventStack {

  def decode(evts: String): EventStack = EventStack(Nil)
}

case class Event(tpe: String, data: Map[String, Any] = Map.empty)
