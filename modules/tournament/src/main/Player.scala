package lila.tournament

import lila.common.LightUser
import lila.hub.LightTeam.TeamID
import lila.rating.Perf
import lila.user.{ Perfs, User }

private[tournament] case class Player(
    _id: Player.ID, // random
    tourId: Tournament.ID,
    userId: User.ID,
    rating: Int,
    provisional: Boolean,
    withdraw: Boolean = false,
    score: Int = 0,
    fire: Boolean = false,
    performance: Int = 0,
    team: Option[TeamID] = None
) {

  def id = _id

  def active = !withdraw

  def is(uid: User.ID): Boolean  = uid == userId
  def is(user: User): Boolean    = is(user.id)
  def is(other: Player): Boolean = is(other.userId)

  def doWithdraw = copy(withdraw = true)
  def unWithdraw = copy(withdraw = false)

  def magicScore = score * 10000 + (performanceOption | rating)

  def performanceOption = performance > 0 option performance
}

private[tournament] object Player {

  type ID = String

  case class WithUser(player: Player, user: User)

  case class Result(player: Player, lightUser: LightUser, rank: Int)

  private[tournament] def make(
      tourId: Tournament.ID,
      user: User,
      perfLens: Perfs => Perf,
      team: Option[TeamID]
  ): Player =
    new Player(
      _id = lila.common.ThreadLocalRandom.nextString(8),
      tourId = tourId,
      userId = user.id,
      rating = perfLens(user.perfs).intRating,
      provisional = perfLens(user.perfs).provisional,
      team = team
    )
}
