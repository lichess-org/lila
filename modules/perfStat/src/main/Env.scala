package lidraughts.perfStat

import akka.actor._
import com.typesafe.config.Config

import akka.actor._

final class Env(
    config: Config,
    system: ActorSystem,
    lightUser: lidraughts.common.LightUser.GetterSync,
    db: lidraughts.db.Env
) {

  private val settings = new {
    val CollectionPerfStat = config getString "collection.perf_stat"
  }
  import settings._

  lazy val storage = new PerfStatStorage(
    coll = db(CollectionPerfStat)
  )

  lazy val indexer = new PerfStatIndexer(
    storage = storage,
    sequencer = new lidraughts.hub.FutureSequencer(
      system = system,
      executionTimeout = None,
      logger = lidraughts.log("perfStat")
    )
  )

  lazy val jsonView = new JsonView(lightUser)

  def get(user: lidraughts.user.User, perfType: lidraughts.rating.PerfType): Fu[PerfStat] =
    storage.find(user.id, perfType) orElse {
      indexer.userPerf(user, perfType) >> storage.find(user.id, perfType)
    } map (_ | PerfStat.init(user.id, perfType))

  system.lidraughtsBus.subscribeFun('finishGame) {
    case lidraughts.game.actorApi.FinishGame(game, _, _) if !game.aborted =>
      indexer addGame game addFailureEffect { e =>
        lidraughts.log("perfStat").error(s"index game ${game.id}", e)
      }
  }
}

object Env {

  lazy val current: Env = "perfStat" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "perfStat",
    system = lidraughts.common.PlayApp.system,
    lightUser = lidraughts.user.Env.current.lightUserSync,
    db = lidraughts.db.Env.current
  )
}
