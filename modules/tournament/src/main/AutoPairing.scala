package lila.tournament

import chess.{ Black, ByColor, Color, White }
import chess.IntRating
import monocle.syntax.all.*

import lila.core.game.Source
import alleycats.Zero

final class AutoPairing(
    gameRepo: lila.core.game.GameRepo,
    newPlayer: lila.core.game.NewPlayer,
    duelStore: DuelStore,
    lightUserApi: lila.core.user.LightUserApi,
    onStart: lila.core.game.OnStart
)(using Executor):

  def apply(tour: Tournament, pairing: Pairing.WithPlayers, ranking: Ranking): Fu[Game] =
    val clock                              = tour.clock.toClock
    val fen: Option[chess.format.Fen.Full] = tour.position.map(_.into(chess.format.Fen.Full))
    val game = lila.core.game
      .newGame(
        chess = chess
          .Game(
            variantOption = Some {
              if tour.position.isEmpty then tour.variant
              else chess.variant.FromPosition
            },
            fen = fen
          )
          .copy(clock = clock.some),
        players = ByColor(makePlayer(White, pairing.player1), makePlayer(Black, pairing.player2)),
        mode = tour.mode,
        source = Source.Arena,
        pgnImport = None
      )
      .withId(pairing.pairing.gameId)
      .focus(_.metadata.tournamentId)
      .replace(tour.id.some)
      .start
    for
      _ <- gameRepo.insertDenormalized(game)
      _ =
        onStart.exec(game.id)
        given Zero[IntRating] = Zero(IntRating(0))
        duelStore.add(
          tour = tour,
          game = game,
          p1 = usernameOf(pairing.player1) -> ~game.whitePlayer.rating,
          p2 = usernameOf(pairing.player2) -> ~game.blackPlayer.rating,
          ranking = ranking
        )
    yield game

  private def makePlayer(color: Color, player: Player) =
    newPlayer(color, player.userId, player.rating, player.provisional)

  private def usernameOf(player: Player) = lightUserApi.syncFallback(player.userId).name
