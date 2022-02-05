package lila.tournament

import shogi.Color
import lila.game.Game
import lila.user.User

case class Pairing(
    id: Game.ID,
    tourId: Tournament.ID,
    status: shogi.Status,
    user1: User.ID,
    user2: User.ID,
    winner: Option[User.ID],
    plies: Option[Int],
    berserk1: Boolean,
    berserk2: Boolean
) {

  def gameId = id

  def users                                       = List(user1, user2)
  def usersPair                                   = user1 -> user2
  def contains(user: User.ID): Boolean            = user1 == user || user2 == user
  def contains(u1: User.ID, u2: User.ID): Boolean = contains(u1) && contains(u2)
  def notContains(user: User.ID)                  = !contains(user)

  def opponentOf(userId: User.ID) =
    if (userId == user1) user2.some
    else if (userId == user2) user1.some
    else none

  def finished = status >= shogi.Status.Mate
  def playing  = !finished

  def quickFinish      = finished && plies.exists(20 >)
  def quickDraw        = draw && plies.exists(20 >)
  def notSoQuickFinish = finished && plies.exists(14 <=)
  def longGame         = plies.exists(60 <=)

  def wonBy(user: User.ID): Boolean     = winner.has(user)
  def lostBy(user: User.ID): Boolean    = winner.exists(user !=)
  def notLostBy(user: User.ID): Boolean = winner.fold(true)(user ==)
  def draw: Boolean                     = finished && winner.isEmpty

  def colorOf(userId: User.ID): Option[Color] =
    if (userId == user1) Color.Sente.some
    else if (userId == user2) Color.Gote.some
    else none

  def berserkOf(userId: User.ID): Boolean =
    if (userId == user1) berserk1
    else if (userId == user2) berserk2
    else false

  def berserkOf(color: Color) = color.fold(berserk1, berserk2)

  def similar(other: Pairing) = other.contains(user1, user2)
}

private[tournament] object Pairing {

  case class LastOpponents(hash: Map[User.ID, User.ID]) extends AnyVal

  private def make(
      gameId: Game.ID,
      tourId: Tournament.ID,
      u1: User.ID,
      u2: User.ID
  ) =
    new Pairing(
      id = gameId,
      tourId = tourId,
      status = shogi.Status.Created,
      user1 = u1,
      user2 = u2,
      winner = none,
      plies = none,
      berserk1 = false,
      berserk2 = false
    )

  case class Prep(tourId: Tournament.ID, user1: User.ID, user2: User.ID) {
    def toPairing(gameId: Game.ID)(firstGetsSente: Boolean): Pairing =
      if (firstGetsSente) make(gameId, tourId, user1, user2)
      else make(gameId, tourId, user2, user1)
  }

  def prep(tour: Tournament, ps: (Player, Player)) =
    Prep(tour.id, ps._1.userId, ps._2.userId)
  def prep(tour: Tournament, p1: Player, p2: Player) =
    Prep(tour.id, p1.userId, p2.userId)
}
