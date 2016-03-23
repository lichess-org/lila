package lila.round

import chess.Color
import lila.game.{ Game, Progress, Pov, GameRepo }
import lila.memo.AsyncCache
import ornicar.scalalib.Zero

private final class GameProxy(id: String) {

  val enabled = true

  def game: Fu[Option[Game]] = if (enabled) cache else fetch

  def save(progress: Progress): Funit = {
    set(progress.game)
    GameRepo save progress
  }

  def invalidating(f: GameRepo.type => Funit): Funit = f(GameRepo) >>- invalidate

  def bypass(f: GameRepo.type => Funit): Funit = f(GameRepo)

  def set(game: Game): Unit = {
    if (enabled) cache = fuccess(game.some)
  }

  def invalidate: Unit = {
    if (enabled) cache = fetch
  }

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
