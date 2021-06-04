package lila.plan

import play.api.libs.json._
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.{ StandaloneWSClient, StandaloneWSResponse }

import lila.common.config.Secret
import lila.user.User

final private class StripeClient(
    ws: StandaloneWSClient,
    config: StripeClient.Config
)(implicit ec: scala.concurrent.ExecutionContext) {

  import StripeClient._
  import JsonHandlers._

  private val STRIPE_VERSION = "2020-08-27"
  // private val STRIPE_VERSION = "2016-07-06"

  def sessionArgs(customerId: CustomerId, urls: NextUrls): List[(String, Any)] =
    List(
      "payment_method_types[]" -> "card",
      "success_url"            -> urls.success,
      "cancel_url"             -> urls.cancel,
      "customer"               -> customerId.value
    )

  def createOneTimeSession(data: CreateStripeSession): Fu[StripeSession] = {
    val args = sessionArgs(data.customerId, data.urls) ++ List(
      "mode"                                   -> "payment",
      "line_items[0][price_data][product]"     -> config.products.onetime,
      "line_items[0][price_data][currency]"    -> data.checkout.money.currency,
      "line_items[0][price_data][unit_amount]" -> data.checkout.money.toStripeAmount.smallestCurrencyUnit,
      "line_items[0][quantity]"                -> 1
    )
    postOne[StripeSession]("checkout/sessions", args: _*)
  }

  private def recurringPriceArgs(name: String, money: Money) = List(
    s"$name[0][price_data][product]"                   -> config.products.monthly,
    s"$name[0][price_data][currency]"                  -> money.currencyCode,
    s"$name[0][price_data][unit_amount]"               -> money.toStripeAmount.smallestCurrencyUnit,
    s"$name[0][price_data][recurring][interval]"       -> "month",
    s"$name[0][price_data][recurring][interval_count]" -> 1,
    s"$name[0][quantity]"                              -> 1
  )

  def createMonthlySession(data: CreateStripeSession): Fu[StripeSession] = {
    val args = sessionArgs(data.customerId, data.urls) ++
      List("mode" -> "subscription") ++
      recurringPriceArgs("line_items", data.checkout.money)
    postOne[StripeSession]("checkout/sessions", args: _*)
  }

  def createCustomer(user: User, data: Checkout): Fu[StripeCustomer] =
    postOne[StripeCustomer](
      "customers",
      "email"       -> data.email,
      "description" -> user.username,
      "expand[]"    -> "subscriptions"
    )

  def getCustomer(id: CustomerId): Fu[Option[StripeCustomer]] =
    getOne[StripeCustomer](s"customers/${id.value}", "expand[]" -> "subscriptions")

  def updateSubscription(sub: StripeSubscription, money: Money): Fu[StripeSubscription] = {
    val args = recurringPriceArgs("items", money) ++ List(
      "items[0][id]"       -> sub.item.id,
      "proration_behavior" -> "none"
    )
    postOne[StripeSubscription](
      s"subscriptions/${sub.id}",
      args: _*
    )
  }

  def cancelSubscription(sub: StripeSubscription): Fu[StripeSubscription] =
    deleteOne[StripeSubscription](
      s"subscriptions/${sub.id}",
      "at_period_end" -> false
    )

  def getEvent(id: String): Fu[Option[JsObject]] =
    getOne[JsObject](s"events/$id")

  def getNextInvoice(customerId: CustomerId): Fu[Option[StripeInvoice]] =
    getOne[StripeInvoice]("invoices/upcoming", "customer" -> customerId.value)

  def getPaymentMethod(sub: StripeSubscription): Fu[Option[StripePaymentMethod]] =
    sub.default_payment_method ?? { id =>
      getOne[StripePaymentMethod](s"payment_methods/$id")
    }

  def createPaymentUpdateSession(sub: StripeSubscription, urls: NextUrls): Fu[StripeSession] = {
    val args = sessionArgs(sub.customer, urls) ++ List(
      "mode"                                         -> "setup",
      "setup_intent_data[metadata][subscription_id]" -> sub.id
    )
    postOne[StripeSession]("checkout/sessions", args: _*)
  }

  def getSession(id: String): Fu[Option[StripeSessionWithIntent]] =
    getOne[StripeSessionWithIntent](s"checkout/sessions/$id", "expand[]" -> "setup_intent")

  def setCustomerPaymentMethod(customerId: CustomerId, paymentMethod: String): Funit =
    postOne[JsObject](
      s"customers/${customerId.value}",
      "invoice_settings[default_payment_method]" -> paymentMethod
    ).void

  def setSubscriptionPaymentMethod(subscription: StripeSubscription, paymentMethod: String): Funit =
    postOne[JsObject](s"subscriptions/${subscription.id}", "default_payment_method" -> paymentMethod).void

  private def getOne[A: Reads](url: String, queryString: (String, Any)*): Fu[Option[A]] =
    get[A](url, queryString) dmap some recover {
      case _: NotFoundException => None
      case e: DeletedException =>
        play.api.Logger("stripe").warn(e.getMessage)
        None
    }

  private def getList[A: Reads](url: String, queryString: (String, Any)*): Fu[List[A]] =
    get[List[A]](url, queryString)(listReader[A])

  private def postOne[A: Reads](url: String, data: (String, Any)*): Fu[A] = post[A](url, data)

  private def deleteOne[A: Reads](url: String, queryString: (String, Any)*): Fu[A] =
    delete[A](url, queryString)

  private def get[A: Reads](url: String, queryString: Seq[(String, Any)]): Fu[A] = {
    logger.debug(s"GET $url ${debugInput(queryString)}")
    request(url).withQueryStringParameters(fixInput(queryString): _*).get() flatMap response[A]
  }

  private def post[A: Reads](url: String, data: Seq[(String, Any)]): Fu[A] = {
    logger.info(s"POST $url ${debugInput(data)}")
    request(url).post(fixInput(data).toMap) flatMap response[A]
  }

  private def delete[A: Reads](url: String, data: Seq[(String, Any)]): Fu[A] = {
    logger.info(s"DELETE $url ${debugInput(data)}")
    request(url).withQueryStringParameters(fixInput(data): _*).delete() flatMap response[A]
  }

  private def request(url: String) =
    ws.url(s"${config.endpoint}/$url")
      .withHttpHeaders(
        "Authorization"  -> s"Bearer ${config.secretKey.value}",
        "Stripe-Version" -> STRIPE_VERSION
      )

  private def response[A: Reads](res: StandaloneWSResponse): Fu[A] =
    res.status match {
      case 200 =>
        (implicitly[Reads[A]] reads res.body[JsValue]).fold(
          errs =>
            fufail {
              if (isDeleted(res.body[JsValue]))
                new DeletedException(s"[stripe] Upstream resource was deleted: ${res.body}")
              else new Exception(s"[stripe] Can't parse ${res.body} --- $errs")
            },
          fuccess
        )
      case 404 => fufail { new NotFoundException(res.status, s"[stripe] Not found") }
      case status if status >= 400 && status < 500 =>
        (res.body[JsValue] \ "error" \ "message").asOpt[String] match {
          case None        => fufail { new InvalidRequestException(status, res.body) }
          case Some(error) => fufail { new InvalidRequestException(status, error) }
        }
      case status => fufail { new StatusException(status, s"[stripe] Response status: $status") }
    }

  private def isDeleted(js: JsValue): Boolean =
    js.asOpt[JsObject] flatMap { o =>
      (o \ "deleted").asOpt[Boolean]
    } contains true

  private def fixInput(in: Seq[(String, Any)]): Seq[(String, String)] =
    in flatMap {
      case (name, Some(x)) => Some(name -> x.toString)
      case (_, None)       => None
      case (name, x)       => Some(name -> x.toString)
    }

  private def listReader[A: Reads]: Reads[List[A]] = (__ \ "data").read[List[A]]

  private def debugInput(data: Seq[(String, Any)]) =
    fixInput(data) map { case (k, v) => s"$k=$v" } mkString " "
}

object StripeClient {

  class StripeException(msg: String)                      extends Exception(msg)
  class DeletedException(msg: String)                     extends StripeException(msg)
  class StatusException(status: Int, msg: String)         extends StripeException(s"$status $msg")
  class NotFoundException(status: Int, msg: String)       extends StatusException(status, msg)
  class InvalidRequestException(status: Int, msg: String) extends StatusException(status, msg)

  import io.methvin.play.autoconfig._
  private[plan] case class Config(
      endpoint: String,
      @ConfigName("keys.public") publicKey: String,
      @ConfigName("keys.secret") secretKey: Secret,
      products: StripeProducts
  )
  implicit private[plan] val productsLoader     = AutoConfig.loader[StripeProducts]
  implicit private[plan] val stripeConfigLoader = AutoConfig.loader[Config]
}
