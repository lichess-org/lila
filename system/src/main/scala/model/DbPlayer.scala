package lila.system
package model

import lila.chess._

case class DbPlayer(
    id: String,
    color: Color,
    ps: String,
    aiLevel: Option[Int],
    isWinner: Option[Boolean],
    evts: String = "",
    elo: Option[Int]) {

  def eventStack: EventStack = EventStack decode evts

  def newEvts(events: List[Event]): String =
    (eventStack withEvents events).optimize.encode

  def withEvents(events: List[Event]) = copy(
    evts = newEvts(events)
  )

  def encodePieces(allPieces: Iterable[(Pos, Piece, Boolean)]): String =
    allPieces withFilter (_._2.color == color) map {
      case (pos, piece, dead) ⇒ pos.piotr.toString + {
        if (dead) piece.role.forsyth.toUpper
        else piece.role.forsyth
      }
    } mkString " "

  def isAi = aiLevel.isDefined
}
