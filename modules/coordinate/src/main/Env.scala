package lila.coordinate

import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val CollectionScore = config getString "collection.score"

  lazy val api = new CoordinateApi(scoreColl = scoreColl)

  private[coordinate] lazy val scoreColl = db(CollectionScore)
}

object Env {

  lazy val current: Env = "[boot] puzzle" describes new Env(
    config = lila.common.PlayApp loadConfig "puzzle",
    db = lila.db.Env.current)
}
