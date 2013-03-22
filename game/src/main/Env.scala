package lila.game

import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class Env(config: Config, db: lila.db.Env) {

  val CachedNbTtl = config millis "cached.nb.ttl"
  val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  val CollectionGame = config getString "collection.game"
  val CollectionPgn = config getString "collection.pgn"
  val JsPathRaw = config getString "js_path.raw"
  val JsPathCompiled = config getString "js_path.compiled"
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "game",
    db = lila.db.Env.current)
}
