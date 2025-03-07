package lila.tournament

import scalalib.ThreadLocalRandom
import chess.IntRating
import chess.rating.RatingProvisional

import lila.core.LightUser
import lila.core.user.WithPerf

case class Player(
    _id: TourPlayerId, // random
    tourId: TourId,
    userId: UserId,
    rating: IntRating,
    provisional: RatingProvisional,
    withdraw: Boolean = false,
    score: Int = 0,
    fire: Boolean = false,
    performance: Option[IntRating] = None,
    team: Option[TeamId] = None,
    bot: Boolean = false
):

  inline def id = _id

  def active = !withdraw

  def doWithdraw = copy(withdraw = true)
  def unWithdraw = copy(withdraw = false)

  def magicScore = score * 10000 + (performance | rating).value

  def showRating = s"$rating${provisional.yes.so("?")}"

object Player:

  given UserIdOf[Player] = _.userId

  case class WithUser(player: Player, user: User)

  case class Result(player: Player, lightUser: LightUser, rank: Int, sheet: Option[arena.Sheet])

  private[tournament] def make(
      tourId: TourId,
      user: WithPerf,
      team: Option[TeamId],
      bot: Boolean
  ): Player = Player(
    _id = TourPlayerId(ThreadLocalRandom.nextString(8)),
    tourId = tourId,
    userId = user.id,
    rating = user.perf.intRating,
    provisional = user.perf.provisional,
    team = team,
    bot = bot
  )
