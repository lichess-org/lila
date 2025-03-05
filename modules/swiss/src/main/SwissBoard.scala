package lila.swiss

import chess.IntRating

import lila.core.LightUser
import lila.core.chess.Rank

private case class SwissBoard(
    gameId: GameId,
    white: SwissBoard.Player,
    black: SwissBoard.Player
)

private object SwissBoard:
  case class Player(user: LightUser, rank: Rank, rating: IntRating)
  case class WithGame(board: SwissBoard, game: Game)

final private class SwissBoardApi(
    rankingApi: SwissRankingApi,
    lightUserApi: lila.core.user.LightUserApi,
    gameProxy: lila.core.game.GameProxy
)(using Executor):

  private val displayBoards = 6

  private val boardsCache = lila.memo.CacheApi.scaffeine
    .expireAfterWrite(60.minutes)
    .build[SwissId, List[SwissBoard]]()

  def get(id: SwissId): Fu[List[SwissBoard.WithGame]] =
    boardsCache
      .getIfPresent(id)
      .so:
        _.parallel { board =>
          gameProxy
            .game(board.gameId)
            .map2:
              SwissBoard.WithGame(board, _)
        }.dmap(_.flatten)

  def update(data: SwissScoring.Result): Funit =
    import data.*
    rankingApi(swiss).map: ranks =>
      val boards = leaderboard
        .collect:
          case (player, _) if player.present => player
        .flatMap: player =>
          pairings
            .get(player.userId)
            .flatMap:
              _.get(swiss.round)
        .filter(_.isOngoing)
        .distinct
        .take(displayBoards)
        .flatMap: pairing =>
          for
            p1 <- playerMap.get(pairing.white)
            p2 <- playerMap.get(pairing.black)
            u1 <- lightUserApi.sync(p1.userId)
            u2 <- lightUserApi.sync(p2.userId)
            r1 <- ranks.get(p1.userId)
            r2 <- ranks.get(p2.userId)
          yield SwissBoard(
            pairing.gameId,
            white = SwissBoard.Player(u1, r1, p1.rating),
            black = SwissBoard.Player(u2, r2, p2.rating)
          )
      boardsCache.put(swiss.id, boards)
