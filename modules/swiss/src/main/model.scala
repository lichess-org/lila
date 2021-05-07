package lila.swiss

import lila.user.User
import lila.game.Game

object SwissRound {

  case class Number(value: Int) extends AnyVal with IntValue
}

case class MyInfo(rank: Int, gameId: Option[Game.ID], user: User, player: SwissPlayer) {
  def page = (rank + 9) / 10
}

final class GetSwissName(f: Swiss.Id => Option[String]) extends (Swiss.Id => Option[String]) {
  def apply(id: Swiss.Id) = f(id)
}

case class GameView(
    swiss: Swiss,
    ranks: Option[GameRanks]
)
case class GameRanks(whiteRank: Int, blackRank: Int)

case class FeaturedSwisses(
    created: List[Swiss],
    started: List[Swiss]
)

case class SwissFinish(id: Swiss.Id, ranking: Ranking)
