package lila.tournament

import scala.concurrent.duration._

import lila.game.{ Game, Player => GamePlayer, GameRepo, PovRef, Source, PerfPicker }
import lila.user.User

final class AutoPairing(
    duelStore: DuelStore,
    onStart: String => Unit
) {

  def apply(tour: Tournament, pairing: Pairing, usersMap: Map[User.ID, User], ranking: Ranking): Fu[Game] = {
    val user1 = usersMap get pairing.user1 err s"Missing pairing user1 $pairing"
    val user2 = usersMap get pairing.user2 err s"Missing pairing user2 $pairing"
    val clock = tour.clock.toClock
    val perfPicker = PerfPicker.mainOrDefault(
      speed = chess.Speed(clock.config),
      variant = tour.ratingVariant,
      daysPerTurn = none
    )
    val game = Game.make(
      chess = chess.Game(
        variantOption = Some {
          if (tour.position.initial) tour.variant
          else chess.variant.FromPosition
        },
        fen = tour.position.some.filterNot(_.initial).map(_.fen)
      ) |> { g =>
          val turns = g.player.fold(0, 1)
          g.copy(
            clock = clock.some,
            turns = turns,
            startedAtTurn = turns
          )
        },
      whitePlayer = GamePlayer.make(chess.White, user1.some, perfPicker),
      blackPlayer = GamePlayer.make(chess.Black, user2.some, perfPicker),
      mode = tour.mode,
      source = Source.Tournament,
      pgnImport = None
    ).withId(pairing.gameId)
      .withTournamentId(tour.id)
      .start
    (GameRepo insertDenormalized game) >>- {
      onStart(game.id)
      duelStore.add(
        tour = tour,
        game = game,
        p1 = (user1.username -> ~game.whitePlayer.rating),
        p2 = (user2.username -> ~game.blackPlayer.rating),
        ranking = ranking
      )
    } inject game
  }
}
