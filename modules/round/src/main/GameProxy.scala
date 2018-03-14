package lila.round

import chess.Color
import lila.game.{ Game, Progress, Pov, GameRepo }
import ornicar.scalalib.Zero

private final class GameProxy(id: Game.ID) {

  def game: Fu[Option[Game]] = cache

  def save(progress: Progress): Funit = {
    set(progress.game)
    GameRepo save progress
  }

  def invalidating(f: GameRepo.type => Funit): Funit = f(GameRepo) >>- invalidate

  def bypass(f: GameRepo.type => Funit): Funit = f(GameRepo)

  def set(game: Game): Unit =
    cache = fuccess(game.some)

  def invalidate: Unit =
    cache = fetch

  // convenience helpers

  def pov(color: Color) = game.map {
    _ map { Pov(_, color) }
  }

  def playerPov(playerId: String) = game.map {
    _ flatMap { Pov(_, playerId) }
  }

  def withGame[A: Zero](f: Game => Fu[A]): Fu[A] = game.flatMap(_ ?? f)

  // internals

  private var cache: Fu[Option[Game]] = fetch

  private def fetch = GameRepo game id
}

object GameProxy {

  type Save = Progress => Funit
}
