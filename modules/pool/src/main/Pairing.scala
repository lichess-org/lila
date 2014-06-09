package lila.pool

import chess.Color
import lila.game.{ Game, PovRef, IdGenerator }

private[pool] case class Pairing(
    gameId: String,
    status: chess.Status,
    user1: String,
    user2: String,
    turns: Option[Int]) {

  def users = List(user1, user2)
  def usersPair = user1 -> user2
  def contains(user: String) = user1 == user || user2 == user
  def notContains(user: String) = !contains(user)

  def finished = status >= chess.Status.Mate
  def playing = !finished

  def quickLoss = finished && ~turns.map(20 >)

  def opponentOf(user: String): Option[String] =
    if (user == user1) user2.some else if (user == user2) user1.some else none

  def colorOf(userId: String): Option[Color] =
    if (userId == user1) Color.White.some
    else if (userId == user2) Color.Black.some
    else none

  def povRef(userId: String): Option[PovRef] =
    colorOf(userId) map { PovRef(gameId, _) }

  def withStatus(s: chess.Status) = copy(status = s)

  def finish(s: chess.Status, t: Int) = copy(status = s, turns = t.some)
}

case class PairingWithGame(pairing: Pairing, game: Game)

private[pool] object Pairing {

  def apply(user1: String, user2: String): Pairing = new Pairing(
    gameId = IdGenerator.game,
    status = chess.Status.Created,
    user1 = user1,
    user2 = user2,
    turns = none)
}
