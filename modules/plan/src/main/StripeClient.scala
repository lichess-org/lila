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

  def sessionArgs(data: CreateStripeSession): List[(String, Any)] =
    List(
      "payment_method_types[]" -> "card",
      "success_url"            -> data.success_url,
      "cancel_url"             -> data.cancel_url,
      "customer"               -> data.customer_id.value
    )

  def createOneTimeSession(data: CreateStripeSession): Fu[StripeSession] = {
    val args = sessionArgs(data) ++ List(
      "line_items[][name]"     -> "One-time payment",
      "line_items[][quantity]" -> 1,
      "line_items[][amount]"   -> data.checkout.amount.value,
      "line_items[][currency]" -> "usd",
      "line_items[][description]" -> {
        if (data.checkout.amount.value >= 25000)
          s"Lifetime Patron status on lichess.org. <3 Your support makes a huge difference!"
        else
          s"One month of Patron status on lichess.org. <3 Your support makes a huge difference!"
      }
    )
    postOne[StripeSession]("checkout/sessions", args: _*)
  }

  def createMonthlySession(data: CreateStripeSession, plan: StripePlan): Fu[StripeSession] = {
    val args = sessionArgs(data) ++ List("subscription_data[items][][plan]" -> plan.id)
    postOne[StripeSession]("checkout/sessions", args: _*)
  }

  def createCustomer(user: User, data: Checkout): Fu[StripeCustomer] =
    postOne[StripeCustomer](
      "customers",
      "email"       -> data.email,
      "description" -> user.username
    )

  def createAnonCustomer(plan: StripePlan, data: Checkout): Fu[StripeCustomer] =
    postOne[StripeCustomer](
      "customers",
      "plan"        -> plan.id,
      "email"       -> data.email,
      "description" -> "Anonymous"
    )

  def getCustomer(id: CustomerId): Fu[Option[StripeCustomer]] =
    getOne[StripeCustomer](s"customers/${id.value}")

  def updateSubscription(
      sub: StripeSubscription,
      plan: StripePlan
  ): Fu[StripeSubscription] =
    postOne[StripeSubscription](
      s"subscriptions/${sub.id}",
      "plan"    -> plan.id,
      "prorate" -> false
    )

  def cancelSubscription(sub: StripeSubscription): Fu[StripeSubscription] =
    deleteOne[StripeSubscription](
      s"subscriptions/${sub.id}",
      "at_period_end" -> false
    )

  def getEvent(id: String): Fu[Option[JsObject]] =
    getOne[JsObject](s"events/$id")

  def getNextInvoice(customerId: CustomerId): Fu[Option[StripeInvoice]] =
    getOne[StripeInvoice](s"invoices/upcoming", "customer" -> customerId.value)

  def getPastInvoices(customerId: CustomerId): Fu[List[StripeInvoice]] =
    getList[StripeInvoice]("invoices", "customer" -> customerId.value)

  def getPlan(cents: Cents, freq: Freq): Fu[Option[StripePlan]] =
    getOne[StripePlan](s"plans/${StripePlan.make(cents, freq).id}")

  def makePlan(cents: Cents, freq: Freq): Fu[StripePlan] =
    postOne[StripePlan](
      "plans",
      "id"       -> StripePlan.make(cents, freq).id,
      "amount"   -> cents.value,
      "currency" -> "usd",
      "interval" -> "month",
      "name"     -> StripePlan.make(cents, freq).name
    )

  //   def chargeAnonCard(data: Checkout): Funit =
  //     postOne[StripePlan]("charges",
  //       "amount" -> data.cents.value,
  //       "currency" -> "usd",
  //       "source" -> data.source.value,
  //       "description" -> "Anon one-time",
  //       "metadata" -> Map("email" -> data.email),
  //       "receipt_email" -> data.email).void

  // charge without changing the customer plan
  def addOneTime(customer: StripeCustomer, amount: Cents): Funit =
    postOne[StripeCharge](
      "charges",
      "customer"      -> customer.id.value,
      "amount"        -> amount.value,
      "currency"      -> "usd",
      "description"   -> "Monthly customer adds a one-time",
      "receipt_email" -> customer.email
    ).void

  private def getOne[A: Reads](url: String, queryString: (String, Any)*): Fu[Option[A]] =
    get[A](url, queryString) dmap Some.apply recover {
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
    ws.url(s"${config.endpoint}/$url").withHttpHeaders("Authorization" -> s"Bearer ${config.secretKey.value}")

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
      case 404 => fufail { new NotFoundException(s"[stripe] Not found") }
      case x if x >= 400 && x < 500 =>
        (res.body[JsValue] \ "error" \ "message").asOpt[String] match {
          case None        => fufail { new InvalidRequestException(res.body) }
          case Some(error) => fufail { new InvalidRequestException(error) }
        }
      case status => fufail { new StatusException(s"[stripe] Response status: $status") }
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

  class StripeException(msg: String)         extends Exception(msg)
  class DeletedException(msg: String)        extends StripeException(msg)
  class StatusException(msg: String)         extends StripeException(msg)
  class NotFoundException(msg: String)       extends StatusException(msg)
  class InvalidRequestException(msg: String) extends StatusException(msg)

  import io.methvin.play.autoconfig._
  private[plan] case class Config(
      endpoint: String,
      @ConfigName("keys.public") publicKey: String,
      @ConfigName("keys.secret") secretKey: Secret
  )
  implicit private[plan] val configLoader = AutoConfig.loader[Config]
}
