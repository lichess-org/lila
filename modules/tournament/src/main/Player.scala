package lila.tournament

import ornicar.scalalib.ThreadLocalRandom

import lila.common.LightUser
import lila.rating.PerfType
import lila.user.User

private[tournament] case class Player(
    _id: TourPlayerId, // random
    tourId: TourId,
    userId: UserId,
    rating: IntRating,
    provisional: RatingProvisional,
    withdraw: Boolean = false,
    score: Int = 0,
    fire: Boolean = false,
    performance: Int = 0,
    team: Option[TeamId] = None
):

  inline def id = _id

  def active = !withdraw

  def is(uid: UserId): Boolean   = uid == userId
  def is(user: User): Boolean    = is(user.id)
  def is(other: Player): Boolean = is(other.userId)

  def doWithdraw = copy(withdraw = true)
  def unWithdraw = copy(withdraw = false)

  def magicScore = score * 10000 + (performanceOption | rating.value)

  def performanceOption = performance > 0 option performance

private[tournament] object Player:

  case class WithUser(player: Player, user: User)

  case class Result(player: Player, lightUser: LightUser, rank: Int, sheet: Option[arena.Sheet])

  private[tournament] def make(
      tourId: TourId,
      user: User.WithPerf,
      team: Option[TeamId]
  ): Player = Player(
    _id = TourPlayerId(ThreadLocalRandom.nextString(8)),
    tourId = tourId,
    userId = user.id,
    rating = user.perf.intRating,
    provisional = user.perf.provisional,
    team = team
  )
