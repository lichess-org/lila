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

  private val workQueue = new lila.hub.DuctSequencer(maxSize = 16, timeout = 10 seconds, name = "gameStarter")

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
        p1Sente <- userRepo.firstGetsSente(p1.userId, p2.userId)
        (sentePerf, gotePerf)     = if (p1Sente) perf1 -> perf2 else perf2 -> perf1
        (senteMember, goteMember) = if (p1Sente) p1 -> p2 else p2 -> p1
        game = makeGame(
          id,
          pool,
          senteMember.userId -> sentePerf,
          goteMember.userId  -> gotePerf
        ).start
        _ <- gameRepo insertDenormalized game
      } yield {
        onStart(Game.Id(game.id))
        Pairing(
          game,
          senteSri = senteMember.sri,
          goteSri = goteMember.sri
        ).some
      }
    }
  }

  private def makeGame(
      id: Game.ID,
      pool: PoolConfig,
      senteUser: (User.ID, Perf),
      goteUser: (User.ID, Perf)
  ) =
    Game(
      id = id,
      shogi = shogi.Game(
        situation = shogi.Situation(shogi.variant.Standard),
        clock = pool.clock.toClock.some
      ),
      initialSfen = None,
      sentePlayer = Player.make(shogi.Sente, senteUser),
      gotePlayer = Player.make(shogi.Gote, goteUser),
      mode = shogi.Mode.Rated,
      status = shogi.Status.Created,
      daysPerTurn = none,
      metadata = Game.metadata(lila.game.Source.Pool)
    )
}
