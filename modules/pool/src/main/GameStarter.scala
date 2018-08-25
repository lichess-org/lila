package lidraughts.pool

import akka.actor._
import scala.concurrent.Promise

import lidraughts.game.{ Game, Player, GameRepo }
import lidraughts.hub.FutureSequencer
import lidraughts.rating.Perf
import lidraughts.user.{ User, UserRepo }

private final class GameStarter(
    bus: lidraughts.common.Bus,
    onStart: Game.ID => Unit,
    sequencer: FutureSequencer
) {

  def apply(pool: PoolConfig, couples: Vector[MatchMaking.Couple]): Funit = couples.nonEmpty ?? {
    sequencer {
      val userIds = couples.flatMap(_.userIds)
      UserRepo.perfOf(userIds, pool.perfType) flatMap { perfs =>
        couples.map(one(pool, perfs)).sequenceFu.void
      }
    }
  }

  private def one(pool: PoolConfig, perfs: Map[User.ID, Perf])(couple: MatchMaking.Couple): Funit = {
    import couple._
    (perfs.get(p1.userId) |@| perfs.get(p2.userId)).tupled ?? {
      case (perf1, perf2) => for {
        p1White <- UserRepo.firstGetsWhite(p1.userId, p2.userId)
        (whitePerf, blackPerf) = if (p1White) perf1 -> perf2 else perf2 -> perf1
        (whiteMember, blackMember) = if (p1White) p1 -> p2 else p2 -> p1
        g <- makeGame(
          pool,
          whiteMember.userId -> whitePerf,
          blackMember.userId -> blackPerf
        ).withUniqueId
        game = g.start
        _ <- GameRepo insertDenormalized game
      } yield {

        bus.publish(PoolApi.Pairing(
          game,
          whiteUid = whiteMember.uid,
          blackUid = blackMember.uid
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
