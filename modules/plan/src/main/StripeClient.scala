package lila.plan

import play.api.ConfigLoader
import play.api.i18n.Lang
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.{ StandaloneWSClient, StandaloneWSResponse }

import lila.core.config.*

final private class StripeClient(ws: StandaloneWSClient, config: StripeClient.Config)(using
    Executor,
    lila.core.i18n.Translator
):

  import StripeClient.*
  import JsonHandlers.stripe.given
  import WebService.*

  private val STRIPE_VERSION = "2020-08-27"

  def sessionArgs(mode: StripeMode, data: StripeSessionData, urls: NextUrls): List[(String, Matchable)] =
    List(
      "mode"                -> mode,
      "success_url"         -> urls.success,
      "cancel_url"          -> urls.cancel,
      "customer"            -> data.customerId.value,
      "metadata[ipAddress]" -> data.ipOption.fold("?")(_.value)
    ) ::: {
      // https://stripe.com/docs/api/checkout/sessions/create#create_checkout_session-payment_method_types
      (mode == StripeMode.setup).so(List("payment_method_types[]" -> "card"))
    }

  def createOneTimeSession(data: CreateStripeSession)(using Lang): Fu[StripeSession] =
    val args =
      sessionArgs(StripeMode.payment, data, data.urls) ::: List(
        "line_items[0][price_data][product]" -> {
          if data.giftTo.isDefined then config.products.gift
          else config.products.onetime
        },
        "line_items[0][price_data][currency]"    -> data.checkout.money.currency,
        "line_items[0][price_data][unit_amount]" -> StripeAmount(data.checkout.money).value,
        "line_items[0][quantity]"                -> 1,
        "expand[]"                               -> "payment_intent"
      ) ::: data.isLifetime.so {
        List(
          "line_items[0][description]" ->
            lila.core.i18n.I18nKey.patron.payLifetimeOnce.txt(data.checkout.money.display)
        )
      } ::: data.giftTo.so { giftTo =>
        List(
          "metadata[giftTo]" -> giftTo.id.value,
          "payment_intent_data[metadata][giftTo]" -> giftTo.id.value, // so we can get it from charge.metadata.giftTo
          "line_items[0][description]" -> s"Gift Patron wings to ${giftTo.username}"
        )
      }
    postOne[StripeSession]("checkout/sessions", args*)

  private def recurringPriceArgs(name: String, money: Money) = List(
    s"$name[0][price_data][product]"                   -> config.products.monthly,
    s"$name[0][price_data][currency]"                  -> money.currencyCode,
    s"$name[0][price_data][unit_amount]"               -> StripeAmount(money).value,
    s"$name[0][price_data][recurring][interval]"       -> "month",
    s"$name[0][price_data][recurring][interval_count]" -> 1,
    s"$name[0][quantity]"                              -> 1
  )

  def createMonthlySession(data: CreateStripeSession): Fu[StripeSession] =
    val args =
      sessionArgs(StripeMode.subscription, data, data.urls) ++
        recurringPriceArgs("line_items", data.checkout.money)
    postOne[StripeSession]("checkout/sessions", args*)

  def createCustomer(user: User, data: PlanCheckout): Fu[StripeCustomer] =
    postOne[StripeCustomer](
      "customers",
      "email"       -> data.email,
      "description" -> user.username,
      "expand[]"    -> "subscriptions"
    )

  def getCustomer(id: StripeCustomerId): Fu[Option[StripeCustomer]] =
    getOne[StripeCustomer](s"customers/${id.value}", "expand[]" -> "subscriptions")

  def updateSubscription(sub: StripeSubscription, money: Money): Fu[StripeSubscription] =
    val args = recurringPriceArgs("items", money) ++ List(
      "items[0][id]"       -> sub.item.id,
      "proration_behavior" -> "none"
    )
    postOne[StripeSubscription](
      s"subscriptions/${sub.id}",
      args*
    )

  def cancelSubscription(sub: StripeSubscription): Fu[StripeSubscription] =
    deleteOne[StripeSubscription](
      s"subscriptions/${sub.id}",
      "at_period_end" -> false
    )

  def getEvent(id: String): Fu[Option[JsObject]] =
    getOne[JsObject](s"events/$id")

  def getNextInvoice(customerId: StripeCustomerId): Fu[Option[StripeInvoice]] =
    getOne[StripeInvoice]("invoices/upcoming", "customer" -> customerId.value)

  def getPaymentMethod(sub: StripeSubscription): Fu[Option[StripePaymentMethod]] =
    sub.default_payment_method.so { id =>
      getOne[StripePaymentMethod](s"payment_methods/$id")
    }

  def createPaymentUpdateSession(sub: StripeSubscription, urls: NextUrls): Fu[StripeSession] =
    val args = sessionArgs(StripeMode.setup, sub, urls) ++ List(
      "setup_intent_data[metadata][subscription_id]" -> sub.id
    )
    postOne[StripeSession]("checkout/sessions", args*)

  def getSession(id: String): Fu[Option[StripeSessionWithIntent]] =
    getOne[StripeSessionWithIntent](s"checkout/sessions/$id", "expand[]" -> "setup_intent")

  def setCustomerPaymentMethod(customerId: StripeCustomerId, paymentMethod: String): Funit =
    postOne[JsObject](
      s"customers/${customerId.value}",
      "invoice_settings[default_payment_method]" -> paymentMethod
    ).void

  def setCustomerEmail(cus: StripeCustomer, email: EmailAddress): Funit =
    postOne[JsObject](s"customers/${cus.id.value}", "email" -> email.value).void

  def setSubscriptionPaymentMethod(subscription: StripeSubscription, paymentMethod: String): Funit =
    postOne[JsObject](s"subscriptions/${subscription.id}", "default_payment_method" -> paymentMethod).void

  private val logger = lila.plan.logger.branch("stripe")

  private def getOne[A: Reads](url: String, queryString: (String, Matchable)*): Fu[Option[A]] =
    get[A](url, queryString)
      .dmap(some)
      .recover:
        case _: NotFoundException => None
        case e: DeletedException =>
          logger.warn(e.getMessage)
          None

  // private def getList[A: Reads](url: String, queryString: (String, Matchable)*): Fu[List[A]] =
  //   get[List[A]](url, queryString)(using listReader[A])

  private def postOne[A: Reads](url: String, data: (String, Matchable)*): Fu[A] = post[A](url, data)

  private def deleteOne[A: Reads](url: String, queryString: (String, Matchable)*): Fu[A] =
    delete[A](url, queryString)

  private def get[A: Reads](url: String, queryString: Seq[(String, Matchable)]): Fu[A] =
    logger.debug(s"GET $url ${debugInput(queryString)}")
    request(url).withQueryStringParameters(fixInput(queryString)*).get().flatMap(response[A])

  private def post[A: Reads](url: String, data: Seq[(String, Matchable)]): Fu[A] =
    logger.info(s"POST $url ${debugInput(data)}")
    request(url).post(fixInput(data).toMap).flatMap(response[A])

  private def delete[A: Reads](url: String, data: Seq[(String, Matchable)]): Fu[A] =
    logger.info(s"DELETE $url ${debugInput(data)}")
    request(url).withQueryStringParameters(fixInput(data)*).delete().flatMap(response[A])

  private def request(url: String) =
    ws.url(s"${config.endpoint}/$url")
      .withHttpHeaders(
        "Authorization"  -> s"Bearer ${config.secretKey.value}",
        "Stripe-Version" -> STRIPE_VERSION
      )

  private def response[A: Reads](res: StandaloneWSResponse): Fu[A] =
    res.status match
      case 200 =>
        summon[Reads[A]]
          .reads(res.body[JsValue])
          .fold(
            errs =>
              fufail {
                if isDeleted(res.body[JsValue]) then
                  new DeletedException(s"[stripe] Upstream resource was deleted: ${res.body}")
                else new Exception(s"[stripe] Can't parse ${res.body} --- $errs")
              },
            fuccess
          )
      case 404 => fufail { new NotFoundException(res.status, s"[stripe] Not found") }
      case status if status >= 400 && status < 500 =>
        (res.body[JsValue] \ "error" \ "message").asOpt[String] match
          case None        => fufail { new InvalidRequestException(status, res.body) }
          case Some(error) => fufail { new InvalidRequestException(status, error) }
      case status => fufail { new StatusException(status, s"[stripe] Response status: $status") }

  private def isDeleted(js: JsValue): Boolean =
    js.asOpt[JsObject].flatMap { o =>
      (o \ "deleted").asOpt[Boolean]
    } contains true

object StripeClient:

  class StripeException(msg: String)                      extends Exception(msg)
  class DeletedException(msg: String)                     extends StripeException(msg)
  class StatusException(status: Int, msg: String)         extends StripeException(s"$status $msg")
  class NotFoundException(status: Int, msg: String)       extends StatusException(status, msg)
  class InvalidRequestException(status: Int, msg: String) extends StatusException(status, msg)
  object CantUseException extends StripeException("You already donated this week, thank you.")

  import lila.common.config.given
  import lila.common.autoconfig.*

  private[plan] case class Config(
      endpoint: String,
      @ConfigName("keys.public") publicKey: String,
      @ConfigName("keys.secret") secretKey: Secret,
      products: ProductIds
  )
  private[plan] given ConfigLoader[ProductIds] = AutoConfig.loader
  private[plan] given ConfigLoader[Config]     = AutoConfig.loader
