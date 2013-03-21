package lila.hub

import com.typesafe.config.Config
import lila.common.ConfigPimps._

final class Env(config: Config) {

  val MetaTimeout = config duration "meta.timeout"
  val MetaName = config getString "meta.name"

  val meta = new MetaHub(Nil, MetaTimeout)
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "hub")
}
