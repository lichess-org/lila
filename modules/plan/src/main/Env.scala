package lila.plan

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient
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
    db: lila.db.Db,
    ws: StandaloneWSClient,
    timeline: lila.hub.actors.Timeline,
    cacheApi: lila.memo.CacheApi,
    mongoCache: lila.memo.MongoCache.Api,
    lightUserApi: lila.user.LightUserApi,
    userRepo: lila.user.UserRepo,
    settingStore: lila.memo.SettingStore.Builder
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import StripeClient.configLoader
  private val config = appConfig.get[PlanConfig]("plan")(AutoConfig.loader)

  val stripePublicKey = config.stripe.publicKey

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
    cacheApi = cacheApi,
    mongoCache = mongoCache,
    payPalIpnKey = config.payPalIpnKey,
    monthlyGoalApi = monthlyGoalApi
  )

  lazy val webhookHandler = new WebhookHandler(api)

  private lazy val expiration = new Expiration(
    userRepo,
    patronColl,
    notifier
  )

  system.scheduler.scheduleWithFixedDelay(15 minutes, 15 minutes) { () =>
    expiration.run.unit
  }

  def cli =
    new lila.common.Cli {
      def process = {
        case "patron" :: "lifetime" :: user :: Nil =>
          userRepo named user flatMap { _ ?? api.setLifetime } inject "ok"
        // someone donated while logged off.
        // we cannot bind the charge to the user so they get their precious wings.
        // instead, give them a free month.
        case "patron" :: "month" :: user :: Nil =>
          userRepo named user flatMap { _ ?? api.giveMonth } inject "ok"
        case "patron" :: "remove" :: user :: Nil =>
          userRepo named user flatMap { _ ?? api.remove } inject "ok"
      }
    }
}
