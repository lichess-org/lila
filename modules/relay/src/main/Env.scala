package lila.relay

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    studyEnv: lila.study.Env,
    system: ActorSystem
) {

  private val relayColl = db(config getString "collection.relay")

  private val sync = new RelaySync(
    studyApi = studyEnv.api,
    chapterRepo = studyEnv.chapterRepo
  )

  lazy val forms = RelayForm

  val api = new RelayApi(
    coll = relayColl,
    studyApi = studyEnv.api,
    system = system
  )

  lazy val socketHandler = new SocketHandler(
    studyHandler = studyEnv.socketHandler,
    api = api
  )

  private val fetch = system.actorOf(Props(new RelayFetch(
    sync = sync,
    api = api
  )))

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    import lila.study.actorApi._
    def receive = {
      case lila.study.actorApi.StudyLikes(id, likes) => api.setLikes(Relay.Id(id.value), likes)
    }
  })), 'studyLikes)
}

object Env {

  lazy val current: Env = "relay" boot new Env(
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "relay",
    studyEnv = lila.study.Env.current,
    system = lila.common.PlayApp.system
  )
}
