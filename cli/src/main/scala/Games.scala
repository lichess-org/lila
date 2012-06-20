package lila.cli

import lila.core.CoreEnv
import lila.game.{ DbGame, DbPlayer }

import scalaz.effects._
import org.joda.time.DateTime

case class Games(env: CoreEnv) {

  private def repo = env.game.gameRepo

  def fixtures: IO[Unit] = repo.insert {

    import chess.{ Game, White, Black, Mode, Variant, Status }
    import chess.Pos._

    val moves = List(E2 -> E4, D7 -> D5, E4 -> D5, D8 -> D5, B1 -> C3, D5 -> A5, D2 -> D4, C7 -> C6, G1 -> F3, C8 -> G4, C1 -> F4, E7 -> E6, H2 -> H3, G4 -> F3, D1 -> F3, F8 -> B4, F1 -> E2, B8 -> D7, A2 -> A3, E8 -> C8, A3 -> B4, A5 -> A1, E1 -> D2, A1 -> H1, F3 -> C6, B7 -> C6, E2 -> A6)
    val game = moves.foldLeft(success(Game()): Valid[Game]) { (vg, move) ⇒
      vg flatMap { g ⇒ g(move._1, move._2) map (_._1) }
    } ||| { failures ⇒
      throw new RuntimeException(failures.shows)
    }
    DbGame(
      game = game,
      ai = none,
      whitePlayer = DbPlayer(White, none),
      blackPlayer = DbPlayer(Black, none),
      creatorColor = White,
      mode = Mode.Casual,
      variant = Variant.Standard).copy(
        status = Status.Mate,
        updatedAt = DateTime.now.some)
  } map (_ ⇒ Unit)
}
