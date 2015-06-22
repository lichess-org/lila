package lila.donation

import com.typesafe.config.Config
import lila.common.PimpedConfig._
import scala.collection.JavaConversions._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val CollectionDonation = config getString "collection.donation"
  private val MonthlyGoal = config getInt "monthly_goal"
  private val ServerDonors = (config getStringList "server_donors").toList

  def forms = DataForm

  def isDonor(userId: String) =
    if (ServerDonors contains userId) fuccess(true)
    else api.donatedByUser(userId) map (_ >= 200)

  lazy val api = new DonationApi(db(CollectionDonation), MonthlyGoal)
}

object Env {

  lazy val current = "[boot] donation" describes new Env(
    config = lila.common.PlayApp loadConfig "donation",
    db = lila.db.Env.current)
}

