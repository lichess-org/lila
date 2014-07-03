package lila.qa

import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val CollectionQuestion = config getString "collection.question"

  // def forms = DataForm

  // lazy val api = new qaApi(db(Collectionqa), MonthlyGoal)
}

object Env {

  lazy val current = "[boot] donation" describes new Env(
    config = lila.common.PlayApp loadConfig "donation",
    db = lila.db.Env.current)
}
