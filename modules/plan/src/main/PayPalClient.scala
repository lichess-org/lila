package lila.plan

import play.api.ConfigLoader
import play.api.i18n.Lang
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.{ StandaloneWSClient, StandaloneWSResponse, WSAuthScheme }

import java.util.Currency

import lila.common.Json.given
import lila.common.autoconfig.*
import lila.common.config.given
import lila.core.config.*
import lila.memo.CacheApi

final private class PayPalClient(
    ws: StandaloneWSClient,
    config: PayPalClient.Config,
    cacheApi: CacheApi
)(using Executor):

  import PayPalClient.*
  import JsonHandlers.payPal.given

  given moneyWrites: OWrites[Money] = OWrites[Money] { money =>
    Json.obj(
      "currency_code" -> money.currencyCode,
      "value" -> money.amount
    )
  }

  private object path:
    val orders = "v2/checkout/orders"
    def capture(id: PayPalOrderId) = s"$orders/$id/capture"
    val plans = "v1/billing/plans"
    val subscriptions = "v1/billing/subscriptions"
    val token = "v1/oauth2/token"
    val events = "v1/notifications/webhooks-events"

  private val patronMonthProductId = "PATRON-MONTH"

  // todo: proper Eq implementation
  given Eq[Currency] = Eq.fromUniversalEquals

  private val plans = cacheApi(32, "plan.payPal.plans"):
    _.buildAsyncFuture[Currency, PayPalPlan]: cur =>
      getPlans().flatMap:
        _.find(_.currency.has(cur)).fold(createPlan(cur))(fuccess)

  def createOrder(data: CreatePayPalOrder): Fu[PayPalOrderCreated] = postOne[PayPalOrderCreated](
    path.orders,
    Json.obj(
      "intent" -> "CAPTURE",
      "purchase_units" -> List(
        Json.obj(
          "custom_id" -> data.makeCustomId,
          "amount" -> {
            moneyWrites.writes(data.checkout.money) ++ Json.obj(
              "breakdown" -> Json.obj(
                "item_total" -> data.checkout.money
              )
            )
          },
          "items" -> List(
            // TODO replace with product?
            Json.obj(
              "name" -> "One-time Patron",
              "description" -> "Support Lichess and get the Patron wings for one month. Will not renew automatically.",
              "unit_amount" -> data.checkout.money,
              "quantity" -> 1
            )
          )
        )
      )
    )
  )

  // actually triggers the payment for a onetime order
  def captureOrder(id: PayPalOrderId): Fu[PayPalOrder] =
    postOne[PayPalOrder](path.capture(id), Json.obj())

  def createSubscription(checkout: PlanCheckout, user: User): Fu[PayPalSubscriptionCreated] =
    plans.get(checkout.money.currency).flatMap { plan =>
      postOne[PayPalSubscriptionCreated](
        path.subscriptions,
        Json.obj(
          "plan_id" -> plan.id.value,
          "custom_id" -> user.id,
          "plan" -> Json.obj(
            "billing_cycles" -> Json.arr(
              Json.obj(
                "sequence" -> 1,
                "total_cycles" -> 0,
                "pricing_scheme" -> Json.obj(
                  "fixed_price" -> checkout.money
                )
              )
            )
          )
        )
      )
    }

  def cancelSubscription(sub: PayPalSubscription): Funit =
    postOneNoResponse(
      s"${path.subscriptions}/${sub.id}/cancel",
      Json.obj("reason" -> "N/A")
    ).void

  def getOrder(id: PayPalOrderId): Fu[Option[PayPalOrder]] =
    getOne[PayPalOrder](s"${path.orders}/$id")

  def getSubscription(id: PayPalSubscriptionId): Fu[Option[PayPalSubscription]] =
    getOne[PayPalSubscription](s"${path.subscriptions}/$id").recover:
      case CantParseException(json, _)
          if json.str("status").exists(status => status == "CANCELLED" || status == "SUSPENDED") =>
        none

  def getEvent(id: PayPalEventId): Fu[Option[PayPalEvent]] =
    getOne[PayPalEvent](s"${path.events}/$id")

  private val plansPerPage = 20

  def getPlans(page: Int = 1): Fu[List[PayPalPlan]] =
    val current = get[List[PayPalPlan]](
      s"${path.plans}?product_id=$patronMonthProductId&page_size=$plansPerPage&page=$page"
    )(using (__ \ "plans").read[List[PayPalPlan]])
    current
      .flatMap: plans =>
        if plans.size == plansPerPage
        then getPlans(page + 1).map(plans ::: _)
        else fuccess(plans)
      .map(_.filter(_.active))

  def createPlan(currency: Currency): Fu[PayPalPlan] =
    postOne[PayPalPlan](
      path.plans,
      Json.obj(
        "product_id" -> patronMonthProductId,
        "name" -> s"Monthly Patron $currency",
        "description" -> s"Support Lichess and get Patron wings. The subscription is renewed every month. Currency: $currency",
        "status" -> "ACTIVE",
        "billing_cycles" -> Json.arr(
          Json.obj(
            "frequency" -> Json.obj(
              "interval_unit" -> "MONTH",
              "interval_count" -> 1
            ),
            "tenure_type" -> "REGULAR",
            "sequence" -> 1,
            "total_cycles" -> 0,
            "pricing_scheme" -> Json.obj(
              "fixed_price" -> Json.obj(
                "value" -> "1",
                "currency_code" -> currency.getCurrencyCode
              )
            )
          )
        ),
        "payment_preferences" -> Json.obj(
          "auto_bill_outstanding" -> true,
          "payment_failure_threshold" -> 3
        )
      )
    )

  private def getOne[A: Reads](url: String): Fu[Option[A]] =
    get[A](url).dmap(some).recover { case _: NotFoundException =>
      None
    }

  private def get[A: Reads](url: String): Fu[A] =
    logger.debug(s"GET $url")
    request(url).flatMap { _.get().flatMap(response[A]) }

  private def postOne[A: Reads](url: String, data: JsObject): Fu[A] = post[A](url, data)

  private def post[A: Reads](url: String, data: JsObject): Fu[A] =
    logger.info(s"POST $url $data")
    request(url).flatMap { _.post(data).flatMap(response[A]) }

  private def postOneNoResponse(url: String, data: JsObject): Funit =
    logger.info(s"POST $url $data")
    request(url).flatMap(_.post(data)).void

  private val logger = lila.plan.logger.branch("payPal")

  private def request(url: String) = tokenCache.get {}.map { bearer =>
    ws.url(s"${config.endpoint}/$url")
      .withHttpHeaders(
        "Authorization" -> s"Bearer $bearer",
        "Content-Type" -> "application/json",
        "Prefer" -> "return=representation" // important for plans and orders
      )
  }

  private def response[A: Reads](res: StandaloneWSResponse): Fu[A] =
    res.status match
      case 200 | 201 | 204 =>
        (summon[Reads[A]]
          .reads(res.body[JsValue]))
          .fold(
            errs => fufail(new CantParseException(res.body[JsValue], JsError(errs))),
            fuccess
          )
      case 404 => fufail { new NotFoundException(res.status, s"[paypal] Not found") }
      case status if status >= 400 && status < 500 =>
        (res.body[JsValue] \ "error" \ "message").asOpt[String] match
          case None => fufail { new InvalidRequestException(status, res.body) }
          case Some(error) => fufail { new InvalidRequestException(status, error) }
      case status => fufail { new StatusException(status, s"[paypal] Response status: $status") }

  private val tokenCache = cacheApi.unit[AccessToken]:
    _.refreshAfterWrite(10.minutes).buildAsyncFuture: _ =>
      ws.url(s"${config.endpoint}/${path.token}")
        .withAuth(config.publicKey, config.secretKey.value, WSAuthScheme.BASIC)
        .post(Map("grant_type" -> Seq("client_credentials")))
        .flatMap:
          case res if res.status != 200 =>
            fufail(s"PayPal access token ${res.statusText} ${res.body[String].take(200)}")
          case res =>
            (res.body[JsValue] \ "access_token").validate[String] match
              case JsError(err) => fufail(s"PayPal access token ${err} ${res.body[String].take(200)}")
              case JsSuccess(token, _) => fuccess(AccessToken(token))
        .monSuccess(_.plan.paypalCheckout.fetchAccessToken)

object PayPalClient:

  case class AccessToken(value: String) extends StringValue

  class PayPalException(msg: String) extends Exception(msg)
  class StatusException(status: Int, msg: String) extends PayPalException(s"$status $msg")
  class NotFoundException(status: Int, msg: String) extends StatusException(status, msg)
  class InvalidRequestException(status: Int, msg: String) extends StatusException(status, msg)
  case class CantParseException(json: JsValue, err: JsError)
      extends PayPalException(s"[payPal] Can't parse $json --- ${err.errors}")

  private[plan] case class Config(
      endpoint: String,
      @ConfigName("keys.public") publicKey: String,
      @ConfigName("keys.secret") secretKey: Secret
  )
  private[plan] given ConfigLoader[Config] = AutoConfig.loader

  def locale(lang: Lang): Option[String] =
    lang.locale.toString.some.filter(locales.contains)

  // https://developer.paypal.com/docs/archive/checkout/reference/supported-locales/
  private val locales = Set(
    "en_AL",
    "ar_DZ",
    "en_DZ",
    "fr_DZ",
    "es_DZ",
    "zh_DZ",
    "en_AD",
    "fr_AD",
    "es_AD",
    "zh_AD",
    "en_AO",
    "fr_AO",
    "es_AO",
    "zh_AO",
    "en_AI",
    "fr_AI",
    "es_AI",
    "zh_AI",
    "en_AG",
    "fr_AG",
    "es_AG",
    "zh_AG",
    "es_AR",
    "en_AR",
    "en_AM",
    "fr_AM",
    "es_AM",
    "zh_AM",
    "en_AW",
    "fr_AW",
    "es_AW",
    "zh_AW",
    "en_AU",
    "de_AT",
    "en_AT",
    "en_AZ",
    "fr_AZ",
    "es_AZ",
    "zh_AZ",
    "en_BS",
    "fr_BS",
    "es_BS",
    "zh_BS",
    "ar_BH",
    "en_BH",
    "fr_BH",
    "es_BH",
    "zh_BH",
    "en_BB",
    "fr_BB",
    "es_BB",
    "zh_BB",
    "en_BY",
    "en_BE",
    "nl_BE",
    "fr_BE",
    "en_BZ",
    "es_BZ",
    "fr_BZ",
    "zh_BZ",
    "fr_BJ",
    "en_BJ",
    "es_BJ",
    "zh_BJ",
    "en_BM",
    "fr_BM",
    "es_BM",
    "zh_BM",
    "en_BT",
    "es_BO",
    "en_BO",
    "fr_BO",
    "zh_BO",
    "en_BA",
    "en_BW",
    "fr_BW",
    "es_BW",
    "zh_BW",
    "pt_BR",
    "en_BR",
    "en_VG",
    "fr_VG",
    "es_VG",
    "zh_VG",
    "en_BN",
    "en_BG",
    "fr_BF",
    "en_BF",
    "es_BF",
    "zh_BF",
    "fr_BI",
    "en_BI",
    "es_BI",
    "zh_BI",
    "en_KH",
    "fr_CM",
    "en_CM",
    "en_CA",
    "fr_CA",
    "en_CV",
    "fr_CV",
    "es_CV",
    "zh_CV",
    "en_KY",
    "fr_KY",
    "es_KY",
    "zh_KY",
    "fr_TD",
    "en_TD",
    "es_TD",
    "zh_TD",
    "es_CL",
    "en_CL",
    "fr_CL",
    "zh_CL",
    "zh_CN",
    "es_CO",
    "en_CO",
    "fr_CO",
    "zh_CO",
    "fr_KM",
    "en_KM",
    "es_KM",
    "zh_KM",
    "en_CG",
    "fr_CG",
    "es_CG",
    "zh_CG",
    "fr_CD",
    "en_CD",
    "es_CD",
    "zh_CD",
    "en_CK",
    "fr_CK",
    "es_CK",
    "zh_CK",
    "es_CR",
    "en_CR",
    "fr_CR",
    "zh_CR",
    "fr_CI",
    "en_CI",
    "en_HR",
    "en_CY",
    "cs_CZ",
    "en_CZ",
    "fr_CZ",
    "es_CZ",
    "zh_CZ",
    "da_DK",
    "en_DK",
    "fr_DJ",
    "en_DJ",
    "es_DJ",
    "zh_DJ",
    "en_DM",
    "fr_DM",
    "es_DM",
    "zh_DM",
    "es_DO",
    "en_DO",
    "fr_DO",
    "zh_DO",
    "es_EC",
    "en_EC",
    "fr_EC",
    "zh_EC",
    "ar_EG",
    "en_EG",
    "fr_EG",
    "es_EG",
    "zh_EG",
    "es_SV",
    "en_SV",
    "fr_SV",
    "zh_SV",
    "en_ER",
    "fr_ER",
    "es_ER",
    "zh_ER",
    "en_EE",
    "ru_EE",
    "fr_EE",
    "es_EE",
    "zh_EE",
    "en_ET",
    "fr_ET",
    "es_ET",
    "zh_ET",
    "en_FK",
    "fr_FK",
    "es_FK",
    "zh_FK",
    "da_FO",
    "en_FO",
    "fr_FO",
    "es_FO",
    "zh_FO",
    "en_FJ",
    "fr_FJ",
    "es_FJ",
    "zh_FJ",
    "fi_FI",
    "en_FI",
    "fr_FI",
    "es_FI",
    "zh_FI",
    "fr_FR",
    "en_FR",
    "en_GF",
    "fr_GF",
    "es_GF",
    "zh_GF",
    "en_PF",
    "fr_PF",
    "es_PF",
    "zh_PF",
    "fr_GA",
    "en_GA",
    "es_GA",
    "zh_GA",
    "en_GM",
    "fr_GM",
    "es_GM",
    "zh_GM",
    "en_GE",
    "fr_GE",
    "es_GE",
    "zh_GE",
    "de_DE",
    "en_DE",
    "en_GI",
    "fr_GI",
    "es_GI",
    "zh_GI",
    "el_GR",
    "en_GR",
    "fr_GR",
    "es_GR",
    "zh_GR",
    "da_GL",
    "en_GL",
    "fr_GL",
    "es_GL",
    "zh_GL",
    "en_GD",
    "fr_GD",
    "es_GD",
    "zh_GD",
    "en_GP",
    "fr_GP",
    "es_GP",
    "zh_GP",
    "es_GT",
    "en_GT",
    "fr_GT",
    "zh_GT",
    "fr_GN",
    "en_GN",
    "es_GN",
    "zh_GN",
    "en_GW",
    "fr_GW",
    "es_GW",
    "zh_GW",
    "en_GY",
    "fr_GY",
    "es_GY",
    "zh_GY",
    "es_HN",
    "en_HN",
    "fr_HN",
    "zh_HN",
    "en_HK",
    "zh_HK",
    "hu_HU",
    "en_HU",
    "fr_HU",
    "es_HU",
    "zh_HU",
    "en_IS",
    "en_IN",
    "id_ID",
    "en_ID",
    "en_IE",
    "fr_IE",
    "es_IE",
    "zh_IE",
    "he_IL",
    "en_IL",
    "it_IT",
    "en_IT",
    "en_JM",
    "es_JM",
    "fr_JM",
    "zh_JM",
    "ja_JP",
    "en_JP",
    "ar_JO",
    "en_JO",
    "fr_JO",
    "es_JO",
    "zh_JO",
    "en_KZ",
    "fr_KZ",
    "es_KZ",
    "zh_KZ",
    "en_KE",
    "fr_KE",
    "es_KE",
    "zh_KE",
    "en_KI",
    "fr_KI",
    "es_KI",
    "zh_KI",
    "ar_KW",
    "en_KW",
    "fr_KW",
    "es_KW",
    "zh_KW",
    "en_KG",
    "fr_KG",
    "es_KG",
    "zh_KG",
    "en_LA",
    "en_LV",
    "ru_LV",
    "fr_LV",
    "es_LV",
    "zh_LV",
    "en_LS",
    "fr_LS",
    "es_LS",
    "zh_LS",
    "en_LI",
    "fr_LI",
    "es_LI",
    "zh_LI",
    "en_LT",
    "ru_LT",
    "fr_LT",
    "es_LT",
    "zh_LT",
    "en_LU",
    "de_LU",
    "fr_LU",
    "es_LU",
    "zh_LU",
    "en_MK",
    "en_MG",
    "fr_MG",
    "es_MG",
    "zh_MG",
    "en_MW",
    "fr_MW",
    "es_MW",
    "zh_MW",
    "en_MY",
    "en_MV",
    "fr_ML",
    "en_ML",
    "es_ML",
    "zh_ML",
    "en_MT",
    "en_MH",
    "fr_MH",
    "es_MH",
    "zh_MH",
    "en_MQ",
    "fr_MQ",
    "es_MQ",
    "zh_MQ",
    "en_MR",
    "fr_MR",
    "es_MR",
    "zh_MR",
    "en_MU",
    "fr_MU",
    "es_MU",
    "zh_MU",
    "en_YT",
    "fr_YT",
    "es_YT",
    "zh_YT",
    "es_MX",
    "en_MX",
    "en_FM",
    "en_MD",
    "fr_MC",
    "en_MC",
    "en_MN",
    "en_ME",
    "en_MS",
    "fr_MS",
    "es_MS",
    "zh_MS",
    "ar_MA",
    "en_MA",
    "fr_MA",
    "es_MA",
    "zh_MA",
    "en_MZ",
    "fr_MZ",
    "es_MZ",
    "zh_MZ",
    "en_NA",
    "fr_NA",
    "es_NA",
    "zh_NA",
    "en_NR",
    "fr_NR",
    "es_NR",
    "zh_NR",
    "en_NP",
    "nl_NL",
    "en_NL",
    "en_AN",
    "fr_AN",
    "es_AN",
    "zh_AN",
    "en_NC",
    "fr_NC",
    "es_NC",
    "zh_NC",
    "en_NZ",
    "fr_NZ",
    "es_NZ",
    "zh_NZ",
    "es_NI",
    "en_NI",
    "fr_NI",
    "zh_NI",
    "fr_NE",
    "en_NE",
    "es_NE",
    "zh_NE",
    "en_NG",
    "en_NU",
    "fr_NU",
    "es_NU",
    "zh_NU",
    "en_NF",
    "fr_NF",
    "es_NF",
    "zh_NF",
    "no_NO",
    "en_NO",
    "ar_OM",
    "en_OM",
    "fr_OM",
    "es_OM",
    "zh_OM",
    "en_PW",
    "fr_PW",
    "es_PW",
    "zh_PW",
    "es_PA",
    "en_PA",
    "fr_PA",
    "zh_PA",
    "en_PG",
    "fr_PG",
    "es_PG",
    "zh_PG",
    "es_PY",
    "en_PY",
    "es_PE",
    "en_PE",
    "fr_PE",
    "zh_PE",
    "en_PH",
    "en_PN",
    "fr_PN",
    "es_PN",
    "zh_PN",
    "pl_PL",
    "en_PL",
    "pt_PT",
    "en_PT",
    "en_QA",
    "fr_QA",
    "es_QA",
    "zh_QA",
    "ar_QA",
    "en_RE",
    "fr_RE",
    "es_RE",
    "zh_RE",
    "en_RO",
    "fr_RO",
    "es_RO",
    "zh_RO",
    "ru_RU",
    "en_RU",
    "fr_RW",
    "en_RW",
    "es_RW",
    "zh_RW",
    "en_WS",
    "en_SM",
    "fr_SM",
    "es_SM",
    "zh_SM",
    "en_ST",
    "fr_ST",
    "es_ST",
    "zh_ST",
    "ar_SA",
    "en_SA",
    "fr_SA",
    "es_SA",
    "zh_SA",
    "fr_SN",
    "en_SN",
    "es_SN",
    "zh_SN",
    "en_RS",
    "fr_RS",
    "es_RS",
    "zh_RS",
    "fr_SC",
    "en_SC",
    "es_SC",
    "zh_SC",
    "en_SL",
    "fr_SL",
    "es_SL",
    "zh_SL",
    "en_SG",
    "sk_SK",
    "en_SK",
    "fr_SK",
    "es_SK",
    "zh_SK",
    "en_SI",
    "fr_SI",
    "es_SI",
    "zh_SI",
    "en_SB",
    "fr_SB",
    "es_SB",
    "zh_SB",
    "en_SO",
    "fr_SO",
    "es_SO",
    "zh_SO",
    "en_ZA",
    "fr_ZA",
    "es_ZA",
    "zh_ZA",
    "ko_KR",
    "en_KR",
    "es_ES",
    "en_ES",
    "en_LK",
    "en_SH",
    "fr_SH",
    "es_SH",
    "zh_SH",
    "en_KN",
    "fr_KN",
    "es_KN",
    "zh_KN",
    "en_LC",
    "fr_LC",
    "es_LC",
    "zh_LC",
    "en_PM",
    "fr_PM",
    "es_PM",
    "zh_PM",
    "en_VC",
    "fr_VC",
    "es_VC",
    "zh_VC",
    "en_SR",
    "fr_SR",
    "es_SR",
    "zh_SR",
    "en_SJ",
    "fr_SJ",
    "es_SJ",
    "zh_SJ",
    "en_SZ",
    "fr_SZ",
    "es_SZ",
    "zh_SZ",
    "sv_SE",
    "en_SE",
    "de_CH",
    "fr_CH",
    "en_CH",
    "zh_TW",
    "en_TW",
    "en_TJ",
    "fr_TJ",
    "es_TJ",
    "zh_TJ",
    "en_TZ",
    "fr_TZ",
    "es_TZ",
    "zh_TZ",
    "th_TH",
    "en_TH",
    "fr_TG",
    "en_TG",
    "es_TG",
    "zh_TG",
    "en_TO",
    "en_TT",
    "fr_TT",
    "es_TT",
    "zh_TT",
    "ar_TN",
    "en_TN",
    "fr_TN",
    "es_TN",
    "zh_TN",
    "en_TM",
    "fr_TM",
    "es_TM",
    "zh_TM",
    "en_TC",
    "fr_TC",
    "es_TC",
    "zh_TC",
    "en_TV",
    "fr_TV",
    "es_TV",
    "zh_TV",
    "tr_TR",
    "en_TR",
    "en_UG",
    "fr_UG",
    "es_UG",
    "zh_UG",
    "en_UA",
    "ru_UA",
    "fr_UA",
    "es_UA",
    "zh_UA",
    "en_AE",
    "fr_AE",
    "es_AE",
    "zh_AE",
    "ar_AE",
    "en_GB",
    "en_US",
    "fr_US",
    "es_US",
    "zh_US",
    "es_UY",
    "en_UY",
    "fr_UY",
    "zh_UY",
    "en_VU",
    "fr_VU",
    "es_VU",
    "zh_VU",
    "en_VA",
    "fr_VA",
    "es_VA",
    "zh_VA",
    "es_VE",
    "en_VE",
    "fr_VE",
    "zh_VE",
    "en_VN",
    "en_WF",
    "fr_WF",
    "es_WF",
    "zh_WF",
    "ar_YE",
    "en_YE",
    "fr_YE",
    "es_YE",
    "zh_YE",
    "en_ZM",
    "fr_ZM",
    "es_ZM",
    "zh_ZM",
    "en_ZW"
  )
