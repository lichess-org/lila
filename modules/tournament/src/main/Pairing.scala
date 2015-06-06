package lila.tournament

import chess.Color
import lila.game.{ Game, PovRef, IdGenerator }

import org.joda.time.DateTime

case class Pairing(
    gameId: String,
    status: chess.Status,
    user1: String,
    user2: String,
    winner: Option[String],
    turns: Option[Int],
    pairedAt: Option[DateTime],
    berserk1: Int,
    berserk2: Int,
    perf1: Int,
    perf2: Int) {

  def users = List(user1, user2)
  def usersPair = user1 -> user2
  def contains(user: String): Boolean = user1 == user || user2 == user
  def contains(u1: String, u2: String): Boolean = contains(u1) && contains(u2)
  def notContains(user: String) = !contains(user)

  def finished = status >= chess.Status.Mate
  def playing = !finished

  def lostBy(user: String) = winner.??(user !=)
  def quickFinish = finished && turns.??(20 >)
  def quickDraw = draw && turns.??(20 >)
  def notSoQuickFinish = finished && turns.??(14 <=)

  def opponentOf(user: String): Option[String] =
    if (user == user1) user2.some else if (user == user2) user1.some else none

  def wonBy(user: String): Boolean = winner.??(user ==)
  def draw: Boolean = finished && winner.isEmpty

  def colorOf(userId: String): Option[Color] =
    if (userId == user1) Color.White.some
    else if (userId == user2) Color.Black.some
    else none

  def berserkOf(userId: String): Int =
    if (userId == user1) berserk1
    else if (userId == user2) berserk2
    else 0

  def perfOf(userId: String): Int =
    if (userId == user1) perf1
    else if (userId == user2) perf2
    else 0

  def validBerserkOf(userId: String): Int =
    notSoQuickFinish ?? berserkOf(userId)

  def withBerserk(userId: String) = copy(
    berserk1 = if (userId == user1) 1 else berserk1,
    berserk2 = if (userId == user2) 1 else berserk2)

  def povRef(userId: String): Option[PovRef] =
    colorOf(userId) map { PovRef(gameId, _) }

  def withStatus(s: chess.Status) = copy(status = s)

  def finish(g: Game) = copy(
    status = g.status,
    winner = g.winnerUserId,
    turns = g.turns.some,
    perf1 = ~g.whitePlayer.ratingDiff,
    perf2 = ~g.blackPlayer.ratingDiff)
}

private[tournament] object Pairing {

  def apply(u1: String, u2: String): Pairing = apply(u1, u2, None)
  def apply(u1: String, u2: String, pa: DateTime): Pairing = apply(u1, u2, Some(pa))
  def apply(p1: Player, p2: Player): Pairing = apply(p1.id, p2.id)
  def apply(ps: (Player, Player)): Pairing = apply(ps._1, ps._2)

  def apply(u1: String, u2: String, pa: Option[DateTime]): Pairing = new Pairing(
    gameId = IdGenerator.game,
    status = chess.Status.Created,
    user1 = u1,
    user2 = u2,
    winner = none,
    turns = none,
    pairedAt = pa,
    berserk1 = 0,
    berserk2 = 0,
    perf1 = 0,
    perf2 = 0)
}
