package lila.badge

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    notifyApi: lila.notify.NotifyApi,
    db: lila.db.Env) {

  private val CollectionBadge = config getString "collection.badge"

  private lazy val badgeColl = db(CollectionBadge)

  lazy val api = new BadgeApi(
    badgeColl = badgeColl)
}

object Env {

  lazy val current: Env = "badge" boot new Env(
    config = lila.common.PlayApp loadConfig "badge",
    notifyApi = lila.notify.Env.current.api,
    db = lila.db.Env.current)
}
