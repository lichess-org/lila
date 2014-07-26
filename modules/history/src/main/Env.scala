package lila.history

import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val Collectionhistory = config getString "collection.history"

  lazy val api = new HistoryApi(db(Collectionhistory))
}

object Env {

  lazy val current = "[boot] history" describes new Env(
    config = lila.common.PlayApp loadConfig "history",
    db = lila.db.Env.current)
}
