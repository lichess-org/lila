package lila.swiss

import lila.user.User

opaque type SwissRoundNumber = Int
object SwissRoundNumber extends OpaqueInt[SwissRoundNumber]

case class MyInfo(rank: Rank, gameId: Option[GameId], user: User, player: SwissPlayer):
  def page = (rank + 9).value / 10

final class GetSwissName(cache: lila.memo.Syncache[SwissId, Option[String]]):
  export cache.{ sync, async }

case class GameView(
    swiss: Swiss,
    ranks: Option[GameRanks]
)
case class GameRanks(whiteRank: Rank, blackRank: Rank)

case class FeaturedSwisses(
    created: List[Swiss],
    started: List[Swiss]
)

case class SwissFinish(id: SwissId, ranking: Ranking)
