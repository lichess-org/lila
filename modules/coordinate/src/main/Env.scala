package lila.coordinate

import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val CollectionScore = config getString "collection.score"

  lazy val api = new CoordinateApi(scoreColl = scoreColl)

  lazy val forms = DataForm

  private[coordinate] lazy val scoreColl = db(CollectionScore)
}

object Env {

  lazy val current: Env = "[boot] coordinate" describes new Env(
    config = lila.common.PlayApp loadConfig "coordinate",
    db = lila.db.Env.current)
}
