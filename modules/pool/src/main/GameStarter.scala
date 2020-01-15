package lila.pool

import scala.concurrent.duration._

import lila.game.{ Game, GameRepo, IdGenerator, Player }
import lila.common.WorkQueue
import lila.rating.Perf
import lila.user.{ User, UserRepo }

final private class GameStarter(
    userRepo: UserRepo,
    gameRepo: GameRepo,
    idGenerator: IdGenerator,
    onStart: Game.Id => Unit
)(implicit ec: scala.concurrent.ExecutionContext, mat: akka.stream.Materializer) {

  import PoolApi._

  private val workQueue = new WorkQueue(buffer = 16, timeout = 10 seconds, name = "gameStarter")

  def apply(pool: PoolConfig, couples: Vector[MatchMaking.Couple]): Funit = couples.nonEmpty ?? {
    workQueue {
      val userIds = couples.flatMap(_.userIds)
      userRepo.perfOf(userIds, pool.perfType) flatMap { perfs =>
        couples.map(one(pool, perfs)).sequenceFu.map { pairings =>
          lila.common.Bus.publish(Pairings(pairings.flatten.toList), "poolPairings")
        }
      }
    }
  }

  private def one(pool: PoolConfig, perfs: Map[User.ID, Perf])(
      couple: MatchMaking.Couple
  ): Fu[Option[Pairing]] = {
    import couple._
    (perfs.get(p1.userId) |@| perfs.get(p2.userId)).tupled ?? {
      case (perf1, perf2) =>
        for {
          p1White <- userRepo.firstGetsWhite(p1.userId, p2.userId)
          (whitePerf, blackPerf)     = if (p1White) perf1 -> perf2 else perf2 -> perf1
          (whiteMember, blackMember) = if (p1White) p1    -> p2 else p2       -> p1
          game <- makeGame(
            pool,
            whiteMember.userId -> whitePerf,
            blackMember.userId -> blackPerf
          ).start withUniqueId idGenerator
          _ <- gameRepo insertDenormalized game
        } yield {
          onStart(Game.Id(game.id))
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
