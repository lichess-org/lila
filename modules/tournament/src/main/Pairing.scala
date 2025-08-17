package lila.tournament

import chess.variant.*
import scalalib.ThreadLocalRandom

case class Pairing(
    id: GameId,
    tourId: TourId,
    status: chess.Status,
    user1: UserId,
    user2: UserId,
    winner: Option[UserId],
    turns: Option[Int],
    berserk1: Boolean,
    berserk2: Boolean
):

  inline def gameId = id

  def users = List(user1, user2)
  def usersPair = user1 -> user2
  def contains(user: UserId): Boolean = user1 == user || user2 == user
  def contains(u1: UserId, u2: UserId): Boolean = contains(u1) && contains(u2)
  def notContains(user: UserId) = !contains(user)

  def opponentOf(userId: UserId) =
    if userId == user1 then user2.some
    else if userId == user2 then user1.some
    else none

  def finished = status >= chess.Status.Mate
  def playing = !finished

  def quickFinish = finished && turns.exists(20 >)
  def quickDraw = draw && turns.exists(20 >)
  def notSoQuickFinish = finished && turns.exists(14 <=)
  def longGame(variant: Variant) = turns.exists(_ >= (variant match
    case Standard | Chess960 | Horde => 60
    case Antichess | Crazyhouse | KingOfTheHill => 40
    case ThreeCheck | Atomic | RacingKings => 20))

  def wonBy(user: UserId): Boolean = winner.exists(user.is(_))
  def lostBy(user: UserId): Boolean = winner.exists(user.isnt(_))
  def notLostBy(user: UserId): Boolean = winner.forall(user.is(_))
  def draw: Boolean = finished && winner.isEmpty

  def colorOf(userId: UserId): Option[Color] =
    if userId.is(user1) then Color.White.some
    else if userId.is(user2) then Color.Black.some
    else none

  def berserkOf(userId: UserId): Boolean =
    if userId.is(user1) then berserk1
    else if userId.is(user2) then berserk2
    else false

  def berserkOf(color: Color) = color.fold(berserk1, berserk2)

  def similar(other: Pairing) = other.contains(user1, user2)

private[tournament] object Pairing:

  case class LastOpponents(hash: Map[UserId, UserId]) extends AnyVal

  case class WithPlayers(pairing: Pairing, player1: Player, player2: Player)

  private def make(
      gameId: GameId,
      tourId: TourId,
      u1: UserId,
      u2: UserId
  ) = Pairing(
    id = gameId,
    tourId = tourId,
    status = chess.Status.Created,
    user1 = u1,
    user2 = u2,
    winner = none,
    turns = none,
    berserk1 = false,
    berserk2 = false
  )

  case class Prep(player1: Player, player2: Player):
    def toPairing(tourId: TourId, gameId: GameId): Pairing.WithPlayers =
      WithPlayers(make(gameId, tourId, player1.userId, player2.userId), player1, player2)

  def prepWithColor(p1: RankedPlayerWithColorHistory, p2: RankedPlayerWithColorHistory) =
    if p1.colorHistory.firstGetsWhite(p2.colorHistory)(() => ThreadLocalRandom.nextBoolean()) then
      Prep(p1.player, p2.player)
    else Prep(p2.player, p1.player)

  def prepWithRandomColor(p1: Player, p2: Player) =
    if ThreadLocalRandom.nextBoolean() then Prep(p1, p2) else Prep(p2, p1)
