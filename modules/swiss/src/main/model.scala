package lila.swiss

import lila.core.chess.Rank

opaque type SwissRoundNumber = Int
object SwissRoundNumber extends RelaxedOpaqueInt[SwissRoundNumber]

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
):
  def teamIds: Set[TeamId] = (created ::: started).view.map(_.teamId).toSet
