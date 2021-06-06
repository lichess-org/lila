package lila.plan

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration._

import lila.common.config._
import lila.common.Strings
import lila.memo.SettingStore.Strings._

@Module
private class PlanConfig(
    @ConfigName("collection.patron") val patronColl: CollName,
    @ConfigName("collection.charge") val chargeColl: CollName,
    val stripe: StripeClient.Config,
    val oer: CurrencyApi.Config,
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
    system: akka.actor.ActorSystem,
    mode: play.api.Mode
) {

  import StripeClient.stripeConfigLoader
  import CurrencyApi.currencyConfigLoader
  private val config = appConfig.get[PlanConfig]("plan")(AutoConfig.loader)

  val stripePublicKey = config.stripe.publicKey

  val donationGoalSetting = settingStore[Int](
    "donationGoal",
    default = 0,
    text = "Monthly donation goal in USD from https://lichess.org/costs".some
  )

  val paymentMethodsSetting = settingStore[Strings](
    "paymentMethods",
    default = Strings(List("card")),
    text = "Stripe payment methods, separated by commas".some
  )

  private lazy val patronColl = db(config.patronColl)
  private lazy val chargeColl = db(config.chargeColl)

  lazy val stripePaymentMethods: StripePaymentMethods = wire[StripePaymentMethods]

  private lazy val stripeClient: StripeClient = wire[StripeClient]

  lazy val currencyApi: CurrencyApi = wire[CurrencyApi]

  lazy val priceApi: PlanPricingApi = wire[PlanPricingApi]

  lazy val checkoutForm = wire[CheckoutForm]

  private lazy val notifier: PlanNotifier = wire[PlanNotifier]

  private lazy val monthlyGoalApi = new MonthlyGoalApi(
    getGoal = () => Usd(donationGoalSetting.get()),
    chargeColl = chargeColl
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
    monthlyGoalApi = monthlyGoalApi,
    currencyApi = currencyApi,
    pricingApi = priceApi
  )

  lazy val webhookHandler = new WebhookHandler(api)

  private lazy val expiration = new Expiration(
    userRepo,
    patronColl,
    notifier
  )

  system.scheduler.scheduleWithFixedDelay(5 minutes, 5 minutes) { () =>
    expiration.run.unit
  }

  def cli =
    new lila.common.Cli {
      def process = {
        case "patron" :: "lifetime" :: user :: Nil =>
          userRepo named user flatMap { _ ?? api.setLifetime } inject "ok"
        case "patron" :: "month" :: user :: Nil =>
          userRepo named user flatMap { _ ?? api.freeMonth } inject "ok"
        case "patron" :: "remove" :: user :: Nil =>
          userRepo named user flatMap { _ ?? api.remove } inject "ok"
      }
    }
}
