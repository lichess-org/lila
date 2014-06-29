package lila.donation

import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val CollectionDonation = config getString "collection.donation"

  lazy val api = new DonationApi(db(CollectionDonation))
}

object Env {

  lazy val current = "[boot] donation" describes new Env(
    config = lila.common.PlayApp loadConfig "donation",
    db = lila.db.Env.current)
}

