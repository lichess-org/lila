package lidraughts.pool

import akka.actor._
import scala.concurrent.Promise

import lidraughts.game.{ Game, Player, GameRepo }
import lidraughts.hub.Sequencer
import lidraughts.rating.Perf
import lidraughts.user.{ User, UserRepo }

private final class GameStarter(
    bus: lidraughts.common.Bus,
    onStart: Game.ID => Unit,
    sequencer: ActorRef
) {

  def apply(pool: PoolConfig, couples: Vector[MatchMaking.Couple]): Funit = {
    val promise = Promise[Unit]()
    sequencer ! Sequencer.work(all(pool, couples), promise.some)
    promise.future
  }

  private def all(pool: PoolConfig, couples: Vector[MatchMaking.Couple]): Funit =
    couples.nonEmpty ?? {
      val userIds = couples.flatMap(_.userIds)
      UserRepo.perfOf(userIds, pool.perfType) flatMap { perfs =>
        couples.map(one(pool, perfs)).sequenceFu.void
      }
    }

  private def one(pool: PoolConfig, perfs: Map[User.ID, Perf])(couple: MatchMaking.Couple): Funit = {
    import couple._
    (perfs.get(p1.userId) |@| perfs.get(p2.userId)).tupled ?? {
      case (perf1, perf2) => for {
        p1White <- UserRepo.firstGetsWhite(p1.userId, p2.userId)
        (whitePerf, blackPerf) = p1White.fold(perf1 -> perf2, perf2 -> perf1)
        (whiteMember, blackMember) = p1White.fold(p1 -> p2, p2 -> p1)
        game = makeGame(
          pool,
          whiteMember.userId -> whitePerf,
          blackMember.userId -> blackPerf
        ).start
        _ <- GameRepo insertDenormalized game
      } yield {

        bus.publish(PoolApi.Pairing(
          game,
          whiteUid = whiteMember.socketId,
          blackUid = blackMember.socketId
        ), 'poolGame)

        onStart(game.id)
      }
    }
  }

  private def makeGame(
    pool: PoolConfig,
    whiteUser: (User.ID, Perf),
    blackUser: (User.ID, Perf)
  ) = Game.make(
    draughts = draughts.DraughtsGame(
      situation = draughts.Situation(draughts.variant.Standard),
      clock = pool.clock.toClock.some
    ),
    whitePlayer = Player.make(draughts.White, whiteUser),
    blackPlayer = Player.make(draughts.Black, blackUser),
    mode = draughts.Mode.Rated,
    source = lidraughts.game.Source.Pool,
    pdnImport = None
  )
}
