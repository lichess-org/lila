package lila.plan

import play.api.libs.json._
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.WSAuthScheme
import play.api.libs.ws.{ StandaloneWSClient, StandaloneWSResponse }
import scala.concurrent.duration._

import lila.common.config
import lila.common.WebService
import lila.memo.CacheApi
import lila.user.User
import java.util.Currency
import play.api.libs.ws.StandaloneWSRequest

final private class PayPalClient(
    ws: StandaloneWSClient,
    config: PayPalClient.Config,
    cacheApi: CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import PayPalClient._
  import JsonHandlers._
  import JsonHandlers.payPal._
  import WebService._

  implicit private val moneyWrites = OWrites[Money] { money =>
    Json.obj(
      "currency_code" -> money.currencyCode,
      "value"         -> money.amount
    )
  }

  private object path {
    val orders                     = "v2/checkout/orders"
    def capture(id: PayPalOrderId) = s"$orders/$id/capture"
    val plans                      = "v1/billing/plans"
    val subscriptions              = "v1/billing/subscriptions"
    val token                      = "v1/oauth2/token"
    val events                     = "v1/notifications/webhooks-events"
  }

  private val patronMonthProductId = "PATRON-MONTH"

  private val plans = cacheApi(32, "plan.payPal.plans") {
    _.buildAsyncFuture[Currency, PayPalPlan] { cur =>
      getPlans() flatMap {
        _.find(p => p.currency.has(cur)).fold(createPlan(cur))(fuccess)
      }
    }
  }

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
              "quantity"    -> 1
            )
          )
        )
      )
    )
  )

  // actually triggers the payment for a onetime order
  def captureOrder(id: PayPalOrderId): Fu[PayPalOrder] =
    postOne[PayPalOrder](path capture id, Json.obj())

  def createSubscription(checkout: PlanCheckout, user: User): Fu[PayPalSubscriptionCreated] =
    plans.get(checkout.money.currency) flatMap { plan =>
      postOne[PayPalSubscriptionCreated](
        path.subscriptions,
        Json.obj(
          "plan_id"   -> plan.id.value,
          "custom_id" -> user.id,
          "plan" -> Json.obj(
            "billing_cycles" -> Json.arr(
              Json.obj(
                "sequence"     -> 1,
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
    getOne[PayPalSubscription](s"${path.subscriptions}/$id") recover {
      case CantParseException(json, _) if json.str("status").has("CANCELLED") => none
    }

  def getEvent(id: PayPalEventId): Fu[Option[PayPalEvent]] =
    getOne[PayPalEvent](s"${path.events}/$id")

  private val plansPerPage = 20

  def getPlans(page: Int = 1): Fu[List[PayPalPlan]] =
    get(s"${path.plans}?product_id=$patronMonthProductId&page_size=$plansPerPage&page=$page") {
      (__ \ "plans").read[List[PayPalPlan]]
    }.flatMap { plans =>
      if (plans.size == plansPerPage) getPlans(page + 1).map(plans ::: _)
      else fuccess(plans)
    }.map(_.filter(_.active))

  def createPlan(currency: Currency): Fu[PayPalPlan] =
    postOne[PayPalPlan](
      path.plans,
      Json.obj(
        "product_id" -> patronMonthProductId,
        "name"       -> s"Monthly Patron $currency",
        "description" -> s"Support Lichess and get Patron wings. The subscription is renewed every month. Currency: $currency",
        "status" -> "ACTIVE",
        "billing_cycles" -> Json.arr(
          Json.obj(
            "frequency" -> Json.obj(
              "interval_unit"  -> "MONTH",
              "interval_count" -> 1
            ),
            "tenure_type"  -> "REGULAR",
            "sequence"     -> 1,
            "total_cycles" -> 0,
            "pricing_scheme" -> Json.obj(
              "fixed_price" -> Json.obj(
                "value"         -> "1",
                "currency_code" -> currency.getCurrencyCode
              )
            )
          )
        ),
        "payment_preferences" -> Json.obj(
          "auto_bill_outstanding"     -> true,
          "payment_failure_threshold" -> 3
        )
      )
    )

  private def getOne[A: Reads](url: String): Fu[Option[A]] =
    get[A](url) dmap some recover { case _: NotFoundException =>
      None
    }

  private def get[A: Reads](url: String): Fu[A] = {
    logger.debug(s"GET $url")
    request(url) flatMap { _.get() flatMap response[A] }
  }

  private def postOne[A: Reads](url: String, data: JsObject): Fu[A] = post[A](url, data)

  private def post[A: Reads](url: String, data: JsObject): Fu[A] = {
    logger.info(s"POST $url $data")
    request(url) flatMap { _.post(data) flatMap response[A] }
  }

  private def postOneNoResponse(url: String, data: JsObject): Funit = {
    logger.info(s"POST $url $data")
    request(url) flatMap { _.post(data) } void
  }

  private val logger = lila.plan.logger branch "payPal"

  private def request(url: String) = tokenCache.get {} map { bearer =>
    ws.url(s"${config.endpoint}/$url")
      .withHttpHeaders(
        "Authorization" -> s"Bearer $bearer",
        "Content-Type"  -> "application/json",
        "Prefer"        -> "return=representation" // important for plans and orders
      )
  }

  private def response[A: Reads](res: StandaloneWSResponse): Fu[A] =
    res.status match {
      case 200 | 201 | 204 =>
        (implicitly[Reads[A]] reads res.body[JsValue]).fold(
          errs => fufail(new CantParseException(res.body[JsValue], JsError(errs))),
          fuccess
        )
      case 404 => fufail { new NotFoundException(res.status, s"[paypal] Not found") }
      case status if status >= 400 && status < 500 =>
        (res.body[JsValue] \ "error" \ "message").asOpt[String] match {
          case None        => fufail { new InvalidRequestException(status, res.body) }
          case Some(error) => fufail { new InvalidRequestException(status, error) }
        }
      case status => fufail { new StatusException(status, s"[paypal] Response status: $status") }
    }

  private val tokenCache = cacheApi.unit[AccessToken] {
    _.refreshAfterWrite(10 minutes).buildAsyncFuture { _ =>
      ws.url(s"${config.endpoint}/${path.token}")
        .withAuth(config.publicKey, config.secretKey.value, WSAuthScheme.BASIC)
        .post(Map("grant_type" -> Seq("client_credentials")))
        .flatMap {
          case res if res.status != 200 =>
            fufail(s"PayPal access token ${res.statusText} ${res.body take 200}")
          case res =>
            (res.body[JsValue] \ "access_token").validate[String] match {
              case JsError(err)        => fufail(s"PayPal access token ${err} ${res.body take 200}")
              case JsSuccess(token, _) => fuccess(AccessToken(token))
            }
        }
        .monSuccess(_.plan.paypalCheckout.fetchAccessToken)
    }
  }

  private def debugInput(data: Seq[(String, Any)]) =
    fixInput(data) map { case (k, v) => s"$k=$v" } mkString " "
}

object PayPalClient {

  case class AccessToken(value: String) extends StringValue

  class PayPalException(msg: String)                      extends Exception(msg)
  class StatusException(status: Int, msg: String)         extends PayPalException(s"$status $msg")
  class NotFoundException(status: Int, msg: String)       extends StatusException(status, msg)
  class InvalidRequestException(status: Int, msg: String) extends StatusException(status, msg)
  case class CantParseException(json: JsValue, err: JsError)
      extends PayPalException(s"[payPal] Can't parse $json --- ${err.errors}")

  import io.methvin.play.autoconfig._
  private[plan] case class Config(
      endpoint: String,
      @ConfigName("keys.public") publicKey: String,
      @ConfigName("keys.secret") secretKey: config.Secret
  )
  implicit private[plan] val productsLoader     = AutoConfig.loader[ProductIds]
  implicit private[plan] val payPalConfigLoader = AutoConfig.loader[Config]
}
