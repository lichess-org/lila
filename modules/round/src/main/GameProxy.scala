package lila.round

import chess.Color
import lila.game.{ Game, GameDiff, Progress, Pov, GameRepo }
import ornicar.scalalib.Zero

private final class GameProxy(
    id: Game.ID,
    alwaysPersist: () => Boolean,
    persistIfSpeedIdHigherThan: () => Int
) {

  def game: Fu[Option[Game]] = cache

  def save(progress: Progress): Funit = {
    set(progress.game)
    diffType(progress) match {
      case GameProxy.Skip =>
        dirty = true
        funit
      case GameProxy.Update =>
        GameRepo.update(progress, false)
      case GameProxy.Save =>
        dirty = false
        GameRepo.update(progress, true)
    }
  }

  // update both the cache and the DB
  def persistAndSet(p: GameRepo.type => Funit, s: Game => Game): Funit =
    game.map {
      _ ?? { g => set(s(g)) }
    } >> p(GameRepo)

  def persist(f: GameRepo.type => Funit): Funit = f(GameRepo)

  private def set(game: Game): Unit = {
    cache = fuccess(game.some)
  }

  def invalidate: Unit = {
    cache = fetch
  }

  def onExpire: Funit = dirty.pp(s"expire $id") ?? {
    game flatMap {
      _ ?? { g =>
        dirty = false
        GameRepo.update(Progress(g), true)
      }
    }
  }

  // convenience helpers

  def pov(color: Color) = game.dmap {
    _ map { Pov(_, color) }
  }

  def playerPov(playerId: String) = game.dmap {
    _ flatMap { Pov(_, playerId) }
  }

  def withGame[A: Zero](f: Game => Fu[A]): Fu[A] = game.flatMap(_ ?? f)

  // internals

  private var dirty = false

  private def diffType(p: Progress) =
    if (alwaysPersist() || p.game.isSimul || p.game.speed.id > persistIfSpeedIdHigherThan()) GameProxy.Update
    else if (p.statusChanged) GameProxy.Save
    else GameProxy.Skip

  private[this] var cache: Fu[Option[Game]] = fetch

  private[this] def fetch = GameRepo game id
}

object GameProxy {

  type Save = Progress => Funit

  sealed trait DiffType
  case object Skip extends DiffType
  case object Update extends DiffType
  case object Save extends DiffType
}
