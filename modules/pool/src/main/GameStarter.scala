package lila.pool

import lila.game.{ Game, Player, GameRepo }
import lila.user.{ User, UserRepo }

private final class GameStarter(onStart: Game.ID => Unit) {

  def apply(pool: PoolConfig, pairings: List[Pairing]): Funit =
    pairings.map(one(pool)).sequenceFu.void

  private def one(pool: PoolConfig)(pairing: Pairing): Funit =
    UserRepo.byIds(pairing.members.map(_.userId)) flatMap {
      case List(u1, u2) => for {
        u1White <- UserRepo.firstGetsWhite(u1.id, u2.id)
        (whiteUser, blackUser) = u1White.fold(u1 -> u2, u2 -> u1)
        game = makeGame(pool, whiteUser, blackUser).start
        _ <- GameRepo insertDenormalized game
      } yield {
        onStart(game.id)
        // lila.mon.lobby.hook.join()
        // lila.mon.lobby.hook.acceptedRatedClock(hook.clock.show)()
      }
      case _ => funit
    }

  private def makeGame(pool: PoolConfig, whiteUser: User, blackUser: User) = Game.make(
    game = chess.Game(
      board = chess.Board init chess.variant.Standard,
      clock = pool.clock.some),
    whitePlayer = Player.white.withUser(whiteUser.id, whiteUser.perfs(pool.perfType)),
    blackPlayer = Player.black.withUser(blackUser.id, blackUser.perfs(pool.perfType)),
    mode = chess.Mode.Rated,
    variant = chess.variant.Standard,
    source = lila.game.Source.Pool,
    pgnImport = None)
}
