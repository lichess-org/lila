package lila.swiss

import lila.user.User
import lila.game.Game

object SwissRound {

  case class Number(value: Int) extends AnyVal with IntValue
}

case class SwissBye(
    round: SwissRound.Number,
    player: SwissPlayer.Number
)

// case class LeaderboardPlayer(
//     player: SwissPlayer,
//     pairings: Map[SwissRound.Number, SwissPairing]
// )
// object LeaderboardPlayer {
//   def make(swiss: Swiss, player: SwissPlayer, pairings: List[SwissPairing]) = LeaderboardPlayer(
//     player = player,
//     pairings = pairings.view.map { p =>
//       p.round -> p
//     }.toMap
//   )
// }

case class MyInfo(rank: Int, gameId: Option[Game.ID], user: User, player: SwissPlayer) {
  def page = { math.floor((rank - 1) / 10) + 1 }.toInt
}

final class GetSwissName(f: Swiss.Id => Option[String]) extends (Swiss.Id => Option[String]) {
  def apply(id: Swiss.Id) = f(id)
}
