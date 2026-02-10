package lila.plan

import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.autoconfig.{ *, given }
import lila.common.config.given
import lila.core.config.*
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
    cacheApi: lila.memo.CacheApi,
    mongoCache: lila.memo.MongoCache.Api,
    lightUserApi: lila.core.user.LightUserApi,
    relationApi: lila.core.relation.RelationApi,
    userApi: lila.core.user.UserApi,
    settingStore: lila.memo.SettingStore.Builder,
    ip2proxy: lila.core.security.Ip2ProxyApi,
    routeUrl: RouteUrl
)(using Executor, play.api.Mode, lila.core.i18n.Translator, Scheduler):

  private val config = appConfig.get[PlanConfig]("plan")(using AutoConfig.loader)

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

  private lazy val monthlyGoalApi = MonthlyGoalApi(
    getGoal = () => Usd(donationGoalSetting.get()),
    chargeColl = mongo.charge
  )

  lazy val api: PlanApi = wire[PlanApi]

  lazy val webhook = wire[PlanWebhook]

  PlanExpiration(userApi, mongo.patron, notifier)

  lila.common.Bus.sub[lila.core.user.ChangeEmail]:
    case lila.core.user.ChangeEmail(userId, email) => api.onEmailChange(userId, email)

  lila.common.Cli.handle:
    case "patron" :: "lifetime" :: user :: Nil =>
      userApi.byId(UserStr(user)).flatMapz(api.setLifetime).inject("ok")
    case "patron" :: "gift-month" :: user :: Nil =>
      userApi.byId(UserStr(user)).flatMapz(api.freeMonth).inject("ok")
    case "patron" :: "remove" :: user :: Nil =>
      userApi.byId(UserStr(user)).flatMapz(api.remove).inject("ok")
    case "patron" :: "set-months" :: user :: months :: Nil =>
      months.toIntOption.fold(fuccess("invalid months")): months =>
        userApi.byId(UserStr(user)).flatMapz(api.setMonths(_, months)).inject("ok")

final private class PlanMongo(val patron: Coll, val charge: Coll)
