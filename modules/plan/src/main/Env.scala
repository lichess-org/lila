package lila.plan

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import lila.memo.SettingStore.{ StringReader, Formable }
import play.api.Configuration
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import lila.common.config._

@Module
private class PlanConfig(
    @ConfigName("collection.patron") val patronColl: CollName,
    @ConfigName("collection.charge") val chargeColl: CollName,
    val stripe: StripeClient.Config,
    @ConfigName("paypal.ipn_key") val payPalIpnKey: Secret
)

final class Env(
    appConfig: Configuration,
    db: lila.db.Env,
    ws: WSClient,
    timeline: lila.hub.actors.Timeline,
    notifyApi: lila.notify.NotifyApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    lightUserApi: lila.user.LightUserApi,
    userRepo: lila.user.UserRepo,
    settingStore: lila.memo.SettingStore.Builder
)(implicit system: akka.actor.ActorSystem) {

  import StripeClient.configLoader
  private val config = appConfig.get[PlanConfig]("plan")(AutoConfig.loader)

  private lazy val patronColl = db(config.patronColl)
  private lazy val chargeColl = db(config.chargeColl)

  private lazy val stripeClient: StripeClient = wire[StripeClient]

  private lazy val notifier: PlanNotifier = wire[PlanNotifier]

  private lazy val monthlyGoalApi = new MonthlyGoalApi(
    getGoal = () => Usd(donationGoalSetting.get()),
    chargeColl = chargeColl
  )

  val donationGoalSetting = settingStore[Int](
    "donationGoal",
    default = 0,
    text = "Monthly donation goal in USD from https://lichess.org/costs".some
  )

  lazy val api = new PlanApi(
    stripeClient = stripeClient,
    patronColl = patronColl,
    chargeColl = chargeColl,
    notifier = notifier,
    userRepo = userRepo,
    lightUserApi = lightUserApi,
    asyncCache = asyncCache,
    payPalIpnKey = config.payPalIpnKey,
    monthlyGoalApi = monthlyGoalApi
  )

  private lazy val webhookHandler = new WebhookHandler(api)

  private lazy val expiration = new Expiration(
    userRepo,
    patronColl,
    notifier
  )

  system.scheduler.scheduleWithFixedDelay(15 minutes, 15 minutes) {
    () => expiration.run
  }

  def webhook = webhookHandler.apply _

  def cli = new lila.common.Cli {
    def process = {
      case "patron" :: "lifetime" :: user :: Nil =>
        userRepo named user flatMap { _ ?? api.setLifetime } inject "ok"
      // someone donated while logged off.
      // we cannot bind the charge to the user so they get their precious wings.
      // instead, give them a free month.
      case "patron" :: "month" :: user :: Nil =>
        userRepo named user flatMap { _ ?? api.giveMonth } inject "ok"
    }
  }
}
