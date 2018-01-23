package lila.tournament

import scala.concurrent.duration._

import chess.Color
import lila.game.{ Game, Player => GamePlayer, GameRepo, PovRef, Source, PerfPicker }
import lila.user.User

final class AutoPairing(
    duelStore: DuelStore,
    onStart: String => Unit
) {

  def apply(tour: Tournament, pairing: Pairing, usersMap: Map[User.ID, User], ranking: Ranking): Fu[Game] = {
    val user1 = usersMap get pairing.user1 err s"Missing pairing user1 $pairing"
    val user2 = usersMap get pairing.user2 err s"Missing pairing user2 $pairing"
    val game1 = Game.make(
      chess = chess.Game(
        variantOption = tour.variant.some,
        fen = tour.position.some.filterNot(_.initial).map(_.fen)
      ) |> { g =>
          val turns = g.player.fold(0, 1)
          g.copy(
            clock = tour.clock.toClock.some,
            turns = turns,
            startedAtTurn = turns
          )
        },
      whitePlayer = GamePlayer.white,
      blackPlayer = GamePlayer.black,
      mode = tour.mode,
      source = Source.Tournament,
      pgnImport = None
    )
    val game2 = game1
      .updatePlayer(Color.White, _.withUser(user1.id, PerfPicker.mainOrDefault(game1)(user1.perfs)))
      .updatePlayer(Color.Black, _.withUser(user2.id, PerfPicker.mainOrDefault(game1)(user2.perfs)))
      .withTournamentId(tour.id)
      .withId(pairing.gameId)
      .start
    (GameRepo insertDenormalized game2) >>- {
      onStart(game2.id)
      duelStore.add(
        tour = tour,
        game = game2,
        p1 = (user1.username -> ~game2.whitePlayer.rating),
        p2 = (user2.username -> ~game2.blackPlayer.rating),
        ranking = ranking
      )
    } inject game2
  }
}
