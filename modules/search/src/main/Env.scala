package lidraughts.search

import akka.actor.ActorSystem
import com.typesafe.config.Config

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lidraughts.common.Scheduler
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
    config = lidraughts.common.PlayApp loadConfig "search",
    system = lidraughts.common.PlayApp.system,
    scheduler = lidraughts.common.PlayApp.scheduler
  )
}
