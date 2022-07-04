package lila.plan

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import com.softwaremill.tagging._
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
    val payPal: PayPalClient.Config,
    val oer: CurrencyApi.Config,
    @ConfigName("payPal.ipn_key") val payPalIpnKey: Secret
)

@Module
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

  private val config = appConfig.get[PlanConfig]("plan")(AutoConfig.loader)

  val stripePublicKey = config.stripe.publicKey
  val payPalPublicKey = config.payPal.publicKey

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

  private lazy val patronColl = db(config.patronColl).taggedWith[PatronColl]
  private lazy val chargeColl = db(config.chargeColl).taggedWith[ChargeColl]

  lazy val stripePaymentMethods: StripePaymentMethods = wire[StripePaymentMethods]

  private lazy val stripeClient: StripeClient = wire[StripeClient]

  private lazy val payPalClient: PayPalClient = wire[PayPalClient]

  lazy val currencyApi: CurrencyApi = wire[CurrencyApi]

  lazy val priceApi: PlanPricingApi = wire[PlanPricingApi]

  lazy val checkoutForm = wire[PlanCheckoutForm]

  private lazy val notifier: PlanNotifier = wire[PlanNotifier]

  private lazy val monthlyGoalApi = new MonthlyGoalApi(
    getGoal = () => Usd(donationGoalSetting.get()),
    chargeColl = chargeColl
  )

  lazy val api: PlanApi = wire[PlanApi]

  lazy val webhook = wire[PlanWebhook]

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

private trait PatronColl
private trait ChargeColl
