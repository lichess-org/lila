package lila.round

import chess.Color
import lila.game.{ Game, GameDiff, Progress, Pov, GameRepo }
import ornicar.scalalib.Zero

private final class GameProxy(id: Game.ID) {

  def game: Fu[Option[Game]] = cache

  def save(progress: Progress): Funit = {
    set(progress.game)
    GameRepo save progress
  }

  def saveDiff(progress: Progress, diff: GameDiff.Diff): Funit = {
    set(progress.game)
    GameRepo.saveDiff(progress.origin, diff)
  }

  def invalidating(f: GameRepo.type => Funit): Funit = f(GameRepo) >>- invalidate

  def bypass(f: GameRepo.type => Funit): Funit = f(GameRepo)

  def set(game: Game): Unit =
    cache = fuccess(game.some)

  def invalidate: Unit =
    cache = fetch

  // convenience helpers

  def pov(color: Color) = game.dmap {
    _ map { Pov(_, color) }
  }

  def playerPov(playerId: String) = game.dmap {
    _ flatMap { Pov(_, playerId) }
  }

  def withGame[A: Zero](f: Game => Fu[A]): Fu[A] = game.flatMap(_ ?? f)

  // internals

  private[this] var cache: Fu[Option[Game]] = fetch

  private[this] def fetch = GameRepo game id
}

object GameProxy {

  type Save = Progress => Funit
}
