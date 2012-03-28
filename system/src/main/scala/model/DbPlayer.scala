package lila.system
package model

import lila.chess._

import com.mongodb.DBRef

case class DbPlayer(
    id: String,
    color: Color,
    ps: String,
    aiLevel: Option[Int],
    isWinner: Option[Boolean],
    evts: String = "",
    elo: Option[Int],
    isOfferingDraw: Boolean,
    lastDrawOffer: Option[Int],
    user: Option[DBRef]) {

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

  def userId: Option[String] = user map (_.getId.toString)

  def wins = isWinner getOrElse false

  def finish(winner: Boolean) = copy(
    isWinner = if (winner) Some(true) else None
  )
}
