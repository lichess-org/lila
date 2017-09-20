package lila.relay

import scala.concurrent.duration._
import akka.actor._

final class Env(
    studyApi: lila.study.StudyApi,
    system: ActorSystem
) {

  private val api = new RelayApi(studyApi)

  private val sync = system.actorOf(Props(new RelaySync(
    api = api
  )))
}

object Env {

  lazy val current: Env = "relay" boot new Env(
    studyApi = lila.study.Env.current.api,
    system = lila.common.PlayApp.system
  )
}
