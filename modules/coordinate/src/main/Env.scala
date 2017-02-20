package lila.coordinate

import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env
) {

  private val CollectionScore = config getString "collection.score"

  lazy val api = new CoordinateApi(scoreColl = scoreColl)

  lazy val forms = DataForm

  private[coordinate] lazy val scoreColl = db(CollectionScore)
}

object Env {

  lazy val current: Env = "coordinate" boot new Env(
    config = lila.common.PlayApp loadConfig "coordinate",
    db = lila.db.Env.current
  )
}
