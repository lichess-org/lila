package lila.system
package model

import lila.chess._

case class DbPlayer(
    id: String,
    color: String,
    ps: String,
    aiLevel: Option[Int],
    isWinner: Option[Boolean],
    evts: String = "",
    elo: Option[Int]) {

  def eventStack = EventStack decode evts

  def newEvts(events: List[Event]): String =
    (eventStack withEvents events).optimize.encode

  def isAi = aiLevel.isDefined
}

object DbPlayer {

  def encodePieces(allPieces: Iterable[(Pos, Piece, Boolean)], color: Color): String =
    allPieces withFilter (_._2.color == color) map {
      case (pos, piece, dead) ⇒ pos.piotr.toString + {
        if (dead) piece.role.forsyth.toUpper
        else piece.role.forsyth
      }
    } mkString " "
}
