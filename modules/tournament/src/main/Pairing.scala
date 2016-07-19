package lila.tournament

import chess.Color
import lila.game.{ Game, PovRef, IdGenerator }

import org.joda.time.DateTime

case class Pairing(
    id: String, // game Id
    tourId: String,
    status: chess.Status,
    user1: String,
    user2: String,
    winner: Option[String],
    turns: Option[Int],
    berserk1: Int,
    berserk2: Int,
    initial: Boolean) {

  def gameId = id

  def users = List(user1, user2)
  def usersPair = user1 -> user2
  def contains(user: String): Boolean = user1 == user || user2 == user
  def contains(u1: String, u2: String): Boolean = contains(u1) && contains(u2)
  def notContains(user: String) = !contains(user)

  def opponentOf(userId: String) =
    if (userId == user1) user2.some
    else if (userId == user2) user1.some
    else none

  def finished = status >= chess.Status.Mate
  def playing = !finished

  def quickFinish = finished && turns.??(20 >)
  def quickDraw = draw && turns.??(20 >)
  def notSoQuickFinish = finished && turns.??(14 <=)

  def wonBy(user: String): Boolean = winner.??(user ==)
  def lostBy(user: String): Boolean = winner.??(user !=)
  def draw: Boolean = finished && winner.isEmpty

  def colorOf(userId: String): Option[Color] =
    if (userId == user1) Color.White.some
    else if (userId == user2) Color.Black.some
    else none

  def berserkOf(userId: String): Int =
    if (userId == user1) berserk1
    else if (userId == user2) berserk2
    else 0

  def validBerserkOf(userId: String): Int =
    notSoQuickFinish ?? berserkOf(userId)

  def povRef(userId: String): Option[PovRef] =
    colorOf(userId) map { PovRef(gameId, _) }

  def similar(other: Pairing) = other.contains(user1, user2)

  def setInitial = copy(initial = true)
}

private[tournament] object Pairing {

  case class LastOpponents(hash: Map[String, String]) extends AnyVal

  def apply(tourId: String, u1: String, u2: String): Pairing = new Pairing(
    id = IdGenerator.game,
    tourId = tourId,
    status = chess.Status.Created,
    user1 = u1,
    user2 = u2,
    winner = none,
    turns = none,
    berserk1 = 0,
    berserk2 = 0,
    initial = false)

  case class Prep(tourId: String, user1: String, user2: String) {
    def toPairing(firstGetsWhite: Boolean) =
      if (firstGetsWhite) Pairing(tourId, user1, user2)
      else Pairing(tourId, user2, user1)
  }

  def prep(tour: Tournament, ps: (Player, Player)) = Pairing.Prep(tour.id, ps._1.userId, ps._2.userId)
  def prep(tour: Tournament, u1: String, u2: String) = Pairing.Prep(tour.id, u1, u2)
  def prep(tour: Tournament, p1: Player, p2: Player) = Pairing.Prep(tour.id, p1.userId, p2.userId)
}
