package lila.pool

import akka.actor._
import scala.concurrent.Promise

import lila.game.{ Game, Player, GameRepo }
import lila.hub.Sequencer
import lila.rating.Perf
import lila.user.{ User, UserRepo }

private final class GameStarter(
    bus: lila.common.Bus,
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
    chess = chess.Game(
      situation = chess.Situation(chess.variant.Standard),
      clock = pool.clock.toClock.some
    ),
    whitePlayer = Player.white.withUser(whiteUser._1, whiteUser._2),
    blackPlayer = Player.black.withUser(blackUser._1, blackUser._2),
    mode = chess.Mode.Rated,
    source = lila.game.Source.Pool,
    pgnImport = None
  )
}
