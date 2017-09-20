package lila.relay

import akka.actor._
import scala.concurrent.duration._

final class Env(
    studyEnv: lila.study.Env,
    system: ActorSystem
) {

  private val sync = new RelaySync(
    studyApi = studyEnv.api,
    chapterRepo = studyEnv.chapterRepo
  )

  private val fetch = system.actorOf(Props(new RelayFetch(
    sync = sync,
    getCurrents = () => fuccess(List(
      Relay(
        lila.study.Study.Id("AoUZ6bOS"),
        url = "http://localhost:3000"
      )
    ))
  )))
}

object Env {

  lazy val current: Env = "relay" boot new Env(
    studyEnv = lila.study.Env.current,
    system = lila.common.PlayApp.system
  )
}
