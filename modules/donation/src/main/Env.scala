package lila.donation

import com.typesafe.config.Config
import lila.common.PimpedConfig._
import scala.collection.JavaConversions._

final class Env(
    config: Config,
    db: lila.db.Env,
    bus: lila.common.Bus) {

  private val CollectionDonation = config getString "collection.donation"
  private val WeeklyGoal = config getInt "weekly_goal"

  def forms = DataForm

  lazy val api = new DonationApi(
    db(CollectionDonation),
    WeeklyGoal,
    bus = bus)
}

object Env {

  lazy val current = "donation" boot new Env(
    config = lila.common.PlayApp loadConfig "donation",
    db = lila.db.Env.current,
    bus = lila.common.PlayApp.system.lilaBus)
}

