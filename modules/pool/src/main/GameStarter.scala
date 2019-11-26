package lila.pool

import akka.actor._
import scala.concurrent.Promise

import lila.game.{ Game, Player, GameRepo }
import lila.hub.FutureSequencer
import lila.rating.Perf
import lila.user.{ User, UserRepo }

private final class GameStarter(
    onStart: Game.ID => Unit,
    sequencer: FutureSequencer
) {

  import PoolApi._

  def apply(pool: PoolConfig, couples: Vector[MatchMaking.Couple]): Funit = couples.nonEmpty ?? {
    sequencer {
      val userIds = couples.flatMap(_.userIds)
      UserRepo.perfOf(userIds, pool.perfType) flatMap { perfs =>
        couples.map(one(pool, perfs)).sequenceFu.map { pairings =>
          lila.common.Bus.publish(Pairings(pairings.flatten.toList), 'poolPairings)
        }
      }
    }
  }

  private def one(pool: PoolConfig, perfs: Map[User.ID, Perf])(couple: MatchMaking.Couple): Fu[Option[Pairing]] = {
    import couple._
    (perfs.get(p1.userId) |@| perfs.get(p2.userId)).tupled ?? {
      case (perf1, perf2) => for {
        p1White <- UserRepo.firstGetsWhite(p1.userId, p2.userId)
        (whitePerf, blackPerf) = if (p1White) perf1 -> perf2 else perf2 -> perf1
        (whiteMember, blackMember) = if (p1White) p1 -> p2 else p2 -> p1
        game <- makeGame(
          pool,
          whiteMember.userId -> whitePerf,
          blackMember.userId -> blackPerf
        ).start.withUniqueId
        _ <- GameRepo insertDenormalized game
      } yield {
        onStart(game.id)
        Pairing(
          game,
          whiteSri = whiteMember.sri,
          blackSri = blackMember.sri
        ).some
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
    whitePlayer = Player.make(chess.White, whiteUser),
    blackPlayer = Player.make(chess.Black, blackUser),
    mode = chess.Mode.Rated,
    source = lila.game.Source.Pool,
    pgnImport = None
  )
}
