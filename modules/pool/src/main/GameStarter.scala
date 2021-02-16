package lila.pool

import scala.concurrent.duration._

import lila.game.{ Game, GameRepo, IdGenerator, Player }
import lila.rating.Perf
import lila.user.{ User, UserRepo }

final private class GameStarter(
    userRepo: UserRepo,
    gameRepo: GameRepo,
    idGenerator: IdGenerator,
    onStart: Game.Id => Unit
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import PoolApi._

  private val workQueue = new lila.hub.DuctSequencer(maxSize = 32, timeout = 10 seconds, name = "gameStarter")

  def apply(pool: PoolConfig, couples: Vector[MatchMaking.Couple]): Funit =
    couples.nonEmpty ?? {
      workQueue {
        val userIds = couples.flatMap(_.userIds)
        userRepo.perfOf(userIds, pool.perfType) flatMap { perfs =>
          idGenerator.games(couples.size) flatMap { ids =>
            couples.zip(ids).map((one(pool, perfs) _).tupled).sequenceFu.map { pairings =>
              lila.common.Bus.publish(Pairings(pairings.flatten.toList), "poolPairings")
            }
          }
        }
      }
    }

  private def one(pool: PoolConfig, perfs: Map[User.ID, Perf])(
      couple: MatchMaking.Couple,
      id: Game.ID
  ): Fu[Option[Pairing]] = {
    import couple._
    import cats.implicits._
    (perfs.get(p1.userId), perfs.get(p2.userId)).mapN((_, _)) ?? { case (perf1, perf2) =>
      for {
        p1White <- userRepo.firstGetsWhite(p1.userId, p2.userId)
        (whitePerf, blackPerf)     = if (p1White) perf1 -> perf2 else perf2 -> perf1
        (whiteMember, blackMember) = if (p1White) p1 -> p2 else p2 -> p1
        game = makeGame(
          id,
          pool,
          whiteMember.userId -> whitePerf,
          blackMember.userId -> blackPerf
        ).start
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
      id: Game.ID,
      pool: PoolConfig,
      whiteUser: (User.ID, Perf),
      blackUser: (User.ID, Perf)
  ) =
    Game(
      id = id,
      chess = chess.Game(
        situation = chess.Situation(chess.variant.Standard),
        clock = pool.clock.toClock.some
      ),
      whitePlayer = Player.make(chess.White, whiteUser),
      blackPlayer = Player.make(chess.Black, blackUser),
      mode = chess.Mode.Rated,
      status = chess.Status.Created,
      daysPerTurn = none,
      metadata = Game.metadata(lila.game.Source.Pool)
    )
}
