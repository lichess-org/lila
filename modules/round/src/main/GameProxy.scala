package lila.round

import chess.Color
import lila.game.{ Game, Progress, Pov, GameRepo }
import lila.memo.AsyncCache
import ornicar.scalalib.Zero

private final class GameProxy(id: String) {

  private var cache: Fu[Option[Game]] = fetch

  def game: Fu[Option[Game]] = cache

  def pov(color: Color) = game.map {
    _ map { Pov(_, color) }
  }

  def playerPov(playerId: String) = game.map {
    _ flatMap { Pov(_, playerId) }
  }

  def withGame[A: Zero](f: Game => Fu[A]): Fu[A] = game.flatMap(_ ?? f)

  def save(progress: Progress): Funit = {
    set(progress.game)
    GameRepo save progress
  }

  def update(f: GameRepo.type => Funit) = f(GameRepo) >>- clear

  def bypass(f: GameRepo.type => Funit) = f(GameRepo)

  private def set(game: Game): Unit = {
    cache = fuccess(game.some)
  }

  private def clear = {
    cache = fetch
  }

  private def fetch = GameRepo game id
}

object GameProxy {

  type Save = Progress => Funit
}
