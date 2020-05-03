package lila.swiss

import lila.user.User
import lila.game.Game

case class SwissPlayer(
    _id: SwissPlayer.Id, // random
    swissId: Swiss.Id,
    number: SwissPlayer.Number,
    userId: User.ID,
    rating: Int,
    provisional: Boolean,
    points: Swiss.Points,
    score: Swiss.Score
)

object SwissPlayer {

  case class Id(value: String) extends AnyVal with StringValue

  def makeId = Id(scala.util.Random.alphanumeric take 8 mkString)

  case class Number(value: Int) extends AnyVal with IntValue
}

object SwissRound {

  case class Number(value: Int) extends AnyVal with IntValue
}

case class SwissBye(
    round: SwissRound.Number,
    player: SwissPlayer.Number
)

case class LeaderboardPlayer(
    player: SwissPlayer,
    pairings: Map[SwissRound.Number, SwissPairing]
)
object LeaderboardPlayer {
  def make(swiss: Swiss, player: SwissPairing, pairings: List[Pairing]) = LeaderboardPlayer(
    player = player,
    swiss.allRounds.map { round =>
      round -> pairings.find

case class MyInfo(rank: Int, withdraw: Boolean, gameId: Option[Game.ID], user: User) {
  def page = {
    math.floor((rank - 1) / 10) + 1
  }.toInt
}

case class RankedPlayer(rank: Int, player: SwissPlayer) {

  def is(other: RankedPlayer) = player is other.player

  override def toString = s"$rank. ${player.userId}[${player.rating}]"
}

object RankedPlayer {

  def apply(ranking: Ranking)(player: SwissPlayer): Option[RankedPlayer] =
    ranking get player.userId map { rank =>
      RankedPlayer(rank + 1, player)
    }
}

final class GetSwissName(f: Swiss.Id => Option[String]) extends (Swiss.Id => Option[String]) {
  def apply(id: Swiss.Id) = f(id)
}
