package lila.tournament

import chess.Color
import lila.game.{ PovRef, IdGenerator, Game }
import lila.user.User

case class Pairing(
    id: Game.ID, // game Id
    tourId: Tournament.ID,
    status: chess.Status,
    user1: User.ID,
    user2: User.ID,
    winner: Option[User.ID],
    turns: Option[Int],
    berserk1: Boolean,
    berserk2: Boolean
) {

  def gameId = id

  def users = List(user1, user2)
  def usersPair = user1 -> user2
  def contains(user: User.ID): Boolean = user1 == user || user2 == user
  def contains(u1: User.ID, u2: User.ID): Boolean = contains(u1) && contains(u2)
  def notContains(user: User.ID) = !contains(user)

  def opponentOf(userId: User.ID) =
    if (userId == user1) user2.some
    else if (userId == user2) user1.some
    else none

  def finished = status >= chess.Status.Mate
  def playing = !finished

  def quickFinish = finished && turns.??(20 >)
  def quickDraw = draw && turns.??(20 >)
  def notSoQuickFinish = finished && turns.??(14 <=)

  def wonBy(user: User.ID): Boolean = winner.??(user ==)
  def lostBy(user: User.ID): Boolean = winner.??(user !=)
  def draw: Boolean = finished && winner.isEmpty

  def colorOf(userId: User.ID): Option[Color] =
    if (userId == user1) Color.White.some
    else if (userId == user2) Color.Black.some
    else none

  def berserkOf(userId: User.ID): Boolean =
    if (userId == user1) berserk1
    else if (userId == user2) berserk2
    else false

  def povRef(userId: User.ID): Option[PovRef] =
    colorOf(userId) map { PovRef(gameId, _) }

  def similar(other: Pairing) = other.contains(user1, user2)
}

private[tournament] object Pairing {

  case class LastOpponents(hash: Map[User.ID, User.ID]) extends AnyVal

  private def make(tourId: Tournament.ID, u1: User.ID, u2: User.ID): Fu[Pairing] =
    IdGenerator.game dmap { id =>
      new Pairing(
        id = id,
        tourId = tourId,
        status = chess.Status.Created,
        user1 = u1,
        user2 = u2,
        winner = none,
        turns = none,
        berserk1 = false,
        berserk2 = false
      )
    }

  case class Prep(tourId: Tournament.ID, user1: User.ID, user2: User.ID) {
    def toPairing(firstGetsWhite: Boolean): Fu[Pairing] =
      if (firstGetsWhite) Pairing.make(tourId, user1, user2)
      else Pairing.make(tourId, user2, user1)
  }

  def prep(tour: Tournament, ps: (Player, Player)) = Pairing.Prep(tour.id, ps._1.userId, ps._2.userId)
  def prep(tour: Tournament, u1: User.ID, u2: User.ID) = Pairing.Prep(tour.id, u1, u2)
  def prep(tour: Tournament, p1: Player, p2: Player) = Pairing.Prep(tour.id, p1.userId, p2.userId)
}
