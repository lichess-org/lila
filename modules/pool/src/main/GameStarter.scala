package lila.pool

import akka.actor._

import lila.game.{ Game, Player, GameRepo }
import lila.hub.Sequencer
import lila.user.{ User, UserRepo }

private final class GameStarter(
    bus: lila.common.Bus,
    onStart: Game.ID => Unit,
    sequencer: ActorRef) {

  def apply(pool: PoolConfig, couples: Vector[MatchMaking.Couple]): Unit =
    sequencer ! Sequencer.work(all(pool, couples))

  private def all(pool: PoolConfig, couples: Vector[MatchMaking.Couple]): Funit =
    couples.map(one(pool)).sequenceFu.void

  private def one(pool: PoolConfig)(couple: MatchMaking.Couple): Funit =
    UserRepo.byIds(couple.members.map(_.userId)) flatMap {
      case List(u1, u2) => for {
        u1White <- UserRepo.firstGetsWhite(u1.id, u2.id)
        (whiteUser, blackUser) = u1White.fold(u1 -> u2, u2 -> u1)
        (whiteMember, blackMember) = (whiteUser.id == couple.p1.userId).fold(
          couple.p1 -> couple.p2,
          couple.p2 -> couple.p1)
        game = makeGame(pool, whiteUser, blackUser).start
        _ <- GameRepo insertDenormalized game
      } yield {

        bus.publish(PoolApi.Pairing(
          game,
          whiteUid = whiteMember.socketId,
          blackUid = blackMember.socketId
        ), 'poolGame)

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
