package lila.search

import akka.actor.ActorSystem
import com.typesafe.config.Config

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lila.common.Scheduler
) {

  private val Enabled = config getBoolean "enabled"
  private val Writeable = config getBoolean "writeable"
  private val Endpoint = config getString "endpoint"

  val makeClient = (index: Index) =>
    if (Enabled) new ESClientHttp(Endpoint, index, Writeable)
    else new ESClientStub
}

object Env {

  lazy val current = "search" boot new Env(
    config = lila.common.PlayApp loadConfig "search",
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler
  )
}
