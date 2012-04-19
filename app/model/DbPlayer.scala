package lila
package model

import chess._

import com.mongodb.DBRef

case class DbPlayer(
    id: String,
    color: Color,
    ps: String,
    aiLevel: Option[Int],
    isWinner: Option[Boolean],
    elo: Option[Int],
    isOfferingDraw: Boolean,
    lastDrawOffer: Option[Int],
    user: Option[DBRef],
    blurs: Int) {

  def encodePieces(allPieces: Iterable[(Pos, Piece, Boolean)]): String =
    allPieces withFilter (_._2.color == color) map {
      case (pos, piece, dead) ⇒ pos.piotr.toString + {
        if (dead) piece.role.forsyth.toUpper
        else piece.role.forsyth
      }
    } mkString " "

  def isAi = aiLevel.isDefined

  def isHuman = !isAi

  def userId: Option[String] = user map (_.getId.toString)

  def wins = isWinner getOrElse false

  def finish(winner: Boolean) = copy(
    isWinner = if (winner) Some(true) else None
  )

  def offerDraw(turn: Int) = copy(
    isOfferingDraw = true,
    lastDrawOffer = Some(turn)
  )

  def removeDrawOffer = copy(isOfferingDraw = false)
}
