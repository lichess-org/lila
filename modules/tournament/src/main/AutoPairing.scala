package lila.tournament

import chess.{ Black, Color, White }
import scala.util.chaining._

import lila.game.{ Game, Player => GamePlayer, GameRepo, Source }
import lila.user.User

final class AutoPairing(
    gameRepo: GameRepo,
    duelStore: DuelStore,
    lightUserApi: lila.user.LightUserApi,
    onStart: Game.ID => Unit
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(
      tour: Tournament,
      pairing: Pairing,
      playersMap: Map[User.ID, Player],
      ranking: Ranking
  ): Fu[Game] = {
    val player1 = playersMap get pairing.user1 err s"Missing pairing player1 $pairing"
    val player2 = playersMap get pairing.user2 err s"Missing pairing player2 $pairing"
    val clock   = tour.clock.toClock
    val game = Game
      .make(
        chess = chess.Game(
          variantOption = Some {
            if (tour.position.isEmpty) tour.variant
            else chess.variant.FromPosition
          },
          fen = tour.position
        ) pipe { g =>
          val turns = g.player.fold(0, 1)
          g.copy(
            clock = clock.some,
            turns = turns,
            startedAtTurn = turns
          )
        },
        whitePlayer = makePlayer(White, player1),
        blackPlayer = makePlayer(Black, player2),
        mode = tour.mode,
        source = Source.Tournament,
        pgnImport = None
      )
      .withId(pairing.gameId)
      .withTournamentId(tour.id)
      .start
    (gameRepo insertDenormalized game) >>- {
      onStart(game.id)
      duelStore.add(
        tour = tour,
        game = game,
        p1 = usernameOf(pairing.user1) -> ~game.whitePlayer.rating,
        p2 = usernameOf(pairing.user2) -> ~game.blackPlayer.rating,
        ranking = ranking
      )
    } inject game
  }

  private def makePlayer(color: Color, player: Player) =
    GamePlayer.make(color, player.userId, player.rating, player.provisional)

  private def usernameOf(userId: User.ID) =
    lightUserApi.sync(userId).fold(userId)(_.name)
}
