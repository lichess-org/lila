package lila.relay

import scala.concurrent.duration._
import akka.actor._

final class Env(
    studyEnv: lila.study.Env,
    system: ActorSystem
) {

  private val api = new RelayApi(
    studyApi = studyEnv.api,
    chapterRepo = studyEnv.chapterRepo
  )

  private val sync = system.actorOf(Props(new RelaySync(
    api = api
  )))
}

object Env {

  lazy val current: Env = "relay" boot new Env(
    studyEnv = lila.study.Env.current,
    system = lila.common.PlayApp.system
  )
}
