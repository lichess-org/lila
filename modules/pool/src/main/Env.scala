package lila.pool

import akka.actor._
import com.typesafe.config.Config
import scala.collection.JavaConversions._

import lila.common.PimpedConfig._

final class Env(
    system: akka.actor.ActorSystem,
    config: Config,
    onStart: String => Unit) {

  private val PoolList = (config getStringList "list").toList flatMap PoolConfig.parse

  val pools = new Pools(PoolList, system)
}

object Env {

  lazy val current: Env = "pool" boot new Env(
    system = lila.common.PlayApp.system,
    config = lila.common.PlayApp loadConfig "pool",
    onStart = lila.game.Env.current.onStart)
}

