package lila.swiss

import scala.concurrent.duration._

import lila.common.LightUser
import lila.game.Game

private case class SwissSituation(
    gameId: Game.ID,
    sente: SwissSituation.Player,
    gote: SwissSituation.Player
)

private object SwissSituation {
  case class Player(user: LightUser, rank: Int, rating: Int)
  case class WithGame(board: SwissSituation, game: Game)
}

final private class SwissSituationApi(
    rankingApi: SwissRankingApi,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi,
    gameProxyRepo: lila.round.GameProxyRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val displayBoards = 6

  private val boardsCache = cacheApi.scaffeine
    .expireAfterWrite(60 minutes)
    .build[Swiss.Id, List[SwissSituation]]()

  def apply(id: Swiss.Id): Fu[List[SwissSituation.WithGame]] =
    boardsCache.getIfPresent(id) ?? {
      _.map { board =>
        gameProxyRepo.game(board.gameId) map2 {
          SwissSituation.WithGame(board, _)
        }
      }.sequenceFu
        .dmap(_.flatten)
    }

  def update(data: SwissScoring.Result): Funit =
    data match {
      case SwissScoring.Result(swiss, leaderboard, playerMap, pairings) =>
        rankingApi(swiss) map { ranks =>
          boardsCache
            .put(
              swiss.id,
              leaderboard
                .collect {
                  case (player, _) if player.present => player
                }
                .flatMap { player =>
                  pairings get player.userId flatMap {
                    _ get swiss.round
                  }
                }
                .filter(_.isOngoing)
                .distinct
                .take(displayBoards)
                .flatMap { pairing =>
                  for {
                    p1 <- playerMap get pairing.sente
                    p2 <- playerMap get pairing.gote
                    u1 <- lightUserApi sync p1.userId
                    u2 <- lightUserApi sync p2.userId
                    r1 <- ranks get p1.userId
                    r2 <- ranks get p2.userId
                  } yield SwissSituation(
                    pairing.gameId,
                    sente = SwissSituation.Player(u1, r1, p1.rating),
                    gote = SwissSituation.Player(u2, r2, p2.rating)
                  )
                }
            )
        }
    }
}
