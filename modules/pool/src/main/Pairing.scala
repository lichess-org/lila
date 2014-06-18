package lila.pool

import chess.{ Color, Status }
import lila.game.{ Game, PovRef, IdGenerator }

case class Pairing(
    gameId: String,
    status: Status,
    user1: String,
    user2: String,
    user1RatingDiff: Option[Int],
    user2RatingDiff: Option[Int],
    turns: Int,
    winner: Option[String]) {

  def users = List(user1, user2)
  def usersPair = user1 -> user2
  def contains(user: String) = user1 == user || user2 == user
  def notContains(user: String) = !contains(user)

  def finished = status >= Status.Mate
  def playing = !finished

  def draw = finished && winner.isEmpty
  def drawnBy(userId: String) = draw && contains(userId)
  def wonBy(userId: String) = finished && winner.??(userId ==)
  def lostBy(userId: String) = finished && contains(userId) && winner.??(userId !=)

  def booleanResult = winner match {
    case Some(uid) if user1 == uid => Some(true)
    case Some(uid) if user2 == uid => Some(false)
    case _                         => None
  }

  def opponentOf(user: String): Option[String] =
    if (user == user1) user2.some else if (user == user2) user1.some else none

  def colorOf(userId: String): Option[Color] =
    if (userId == user1) Color.White.some
    else if (userId == user2) Color.Black.some
    else none

  def ratingDiffOf(userId: String): Option[Int] =
    if (userId == user1) user1RatingDiff
    else if (userId == user2) user2RatingDiff
    else none

  def povRef(userId: String): Option[PovRef] =
    colorOf(userId) map { PovRef(gameId, _) }

  def withStatus(s: Status) = copy(status = s)

  def finish(game: Game) =
    copy(
      status = game.status,
      turns = game.turns,
      user1RatingDiff = game.whitePlayer.ratingDiff,
      user2RatingDiff = game.blackPlayer.ratingDiff,
      winner = game.winnerUserId)
}

case class PairingWithGame(pairing: Pairing, game: Game)

private[pool] object Pairing {

  def apply(user1: String, user2: String): Pairing = new Pairing(
    gameId = IdGenerator.game,
    status = Status.Created,
    user1 = user1,
    user2 = user2,
    user1RatingDiff = none,
    user2RatingDiff = none,
    turns = 0,
    winner = none)
}
