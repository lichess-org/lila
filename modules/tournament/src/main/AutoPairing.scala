package lila.tournament

import chess.{ Black, Color, White }
import scala.util.chaining._

import lila.game.{ Game, GameRepo, Player => GamePlayer, Source }
import lila.user.User

final class AutoPairing(
    gameRepo: GameRepo,
    duelStore: DuelStore,
    lightUserApi: lila.user.LightUserApi,
    onStart: Game.ID => Unit
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(tour: Tournament, pairing: Pairing.WithPlayers, ranking: Ranking): Fu[Game] = {
    val clock = tour.clock.toClock
    val game = Game
      .make(
        chess = chess
          .Game(
            variantOption = Some {
              if (tour.position.isEmpty) tour.variant
              else chess.variant.FromPosition
            },
            fen = tour.position
          )
          .copy(clock = clock.some),
        whitePlayer = makePlayer(White, pairing.player1),
        blackPlayer = makePlayer(Black, pairing.player2),
        mode = tour.mode,
        source = Source.Tournament,
        pgnImport = None
      )
      .withId(pairing.pairing.gameId)
      .withTournamentId(tour.id)
      .start
    (gameRepo insertDenormalized game) >>- {
      onStart(game.id)
      duelStore.add(
        tour = tour,
        game = game,
        p1 = usernameOf(pairing.player1.userId) -> ~game.whitePlayer.rating,
        p2 = usernameOf(pairing.player2.userId) -> ~game.blackPlayer.rating,
        ranking = ranking
      )
    } inject game
  }

  private def makePlayer(color: Color, player: Player) =
    GamePlayer.make(color, player.userId, player.rating, player.provisional)

  private def usernameOf(userId: User.ID) =
    lightUserApi.sync(userId).fold(userId)(_.name)
}
