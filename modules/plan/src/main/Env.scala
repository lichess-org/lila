package lila.plan

import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.config.*
import lila.db.dsl.Coll

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
    settingStore: lila.memo.SettingStore.Builder,
    ip2proxy: lila.security.Ip2Proxy
)(using
    ec: Executor,
    system: akka.actor.ActorSystem,
    mode: play.api.Mode
):

  private val config = appConfig.get[PlanConfig]("plan")(AutoConfig.loader)

  val stripePublicKey = config.stripe.publicKey
  val payPalPublicKey = config.payPal.publicKey

  val donationGoalSetting = settingStore[Int](
    "donationGoal",
    default = 0,
    text = "Monthly donation goal in USD from https://lichess.org/costs".some
  )

  private lazy val mongo = PlanMongo(
    patron = db(config.patronColl),
    charge = db(config.chargeColl)
  )

  private lazy val stripeClient: StripeClient = wire[StripeClient]

  private lazy val payPalClient: PayPalClient = wire[PayPalClient]

  lazy val currencyApi: CurrencyApi = wire[CurrencyApi]

  lazy val priceApi: PlanPricingApi = wire[PlanPricingApi]

  lazy val checkoutForm = wire[PlanCheckoutForm]

  private lazy val notifier: PlanNotifier = wire[PlanNotifier]

  private lazy val monthlyGoalApi = new MonthlyGoalApi(
    getGoal = () => Usd(donationGoalSetting.get()),
    chargeColl = mongo.charge
  )

  lazy val api: PlanApi = wire[PlanApi]

  lazy val webhook = wire[PlanWebhook]

  private lazy val expiration = new Expiration(
    userRepo,
    mongo.patron,
    notifier
  )

  system.scheduler.scheduleWithFixedDelay(5 minutes, 5 minutes): () =>
    expiration.run

  lila.common.Bus.subscribeFun("email"):
    case lila.hub.actorApi.user.ChangeEmail(userId, email) => api.onEmailChange(userId, email)

  def cli = new lila.common.Cli:
    def process =
      case "patron" :: "lifetime" :: user :: Nil =>
        userRepo byId UserStr(user) flatMapz api.setLifetime inject "ok"
      case "patron" :: "month" :: user :: Nil =>
        userRepo byId UserStr(user) flatMapz api.freeMonth inject "ok"
      case "patron" :: "remove" :: user :: Nil =>
        userRepo byId UserStr(user) flatMapz api.remove inject "ok"

final private class PlanMongo(val patron: Coll, val charge: Coll)
