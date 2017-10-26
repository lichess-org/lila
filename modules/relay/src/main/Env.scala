package lila.relay

import scala.concurrent.duration._
import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    studyEnv: lila.study.Env,
    system: ActorSystem
) {

  private val relayColl = db(config getString "collection.relay")

  lazy val forms = RelayForm

  val api = new RelayApi(
    coll = relayColl,
    studyApi = studyEnv.api,
    system = system
  )

  private val sync = new RelaySync(
    studyApi = studyEnv.api,
    chapterRepo = studyEnv.chapterRepo
  )

  lazy val socketHandler = new SocketHandler(
    studyHandler = studyEnv.socketHandler,
    api = api
  )

  private val fetch = system.actorOf(Props(new RelayFetch(
    sync = sync,
    api = api,
    chapterRepo = studyEnv.chapterRepo
  )))

  system.scheduler.schedule(1 minute, 1 minute) {
    api.autoStart
  }

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lila.study.actorApi.StudyLikes(id, likes) => api.setLikes(Relay.Id(id.value), likes)
      case lila.hub.actorApi.study.RemoveStudy(studyId, _) => api.onStudyRemove(studyId)
    }
  })), 'studyLikes, 'study)
}

object Env {

  lazy val current: Env = "relay" boot new Env(
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "relay",
    studyEnv = lila.study.Env.current,
    system = lila.common.PlayApp.system
  )
}
