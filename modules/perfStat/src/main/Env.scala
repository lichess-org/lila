package lila.perfStat

import akka.actor._
import com.typesafe.config.Config

import akka.actor._

final class Env(
    config: Config,
    system: ActorSystem,
    lightUser: lila.common.LightUser.GetterSync,
    db: lila.db.Env
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
    sequencer = new lila.hub.FutureSequencer(
      system = system,
      executionTimeout = None,
      logger = lila.log("perfStat")
    )
  )

  lazy val jsonView = new JsonView(lightUser)

  def get(user: lila.user.User, perfType: lila.rating.PerfType): Fu[PerfStat] =
    storage.find(user.id, perfType) orElse {
      indexer.userPerf(user, perfType) >> storage.find(user.id, perfType)
    } map (_ | PerfStat.init(user.id, perfType))

  system.lilaBus.subscribeFun('finishGame) {
    case lila.game.actorApi.FinishGame(game, _, _) if !game.aborted =>
      indexer addGame game addFailureEffect { e =>
        lila.log("perfStat").error(s"index game ${game.id}", e)
      }
  }
}

object Env {

  lazy val current: Env = "perfStat" boot new Env(
    config = lila.common.PlayApp loadConfig "perfStat",
    system = lila.common.PlayApp.system,
    lightUser = lila.user.Env.current.lightUserSync,
    db = lila.db.Env.current
  )
}
