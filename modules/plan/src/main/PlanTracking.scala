package lila.plan

import lila.user.User

import play.api.libs.ws.WS
import play.api.Play.current

final class PlanTracking {

  private val url = "https://www.google-analytics.com/collect"
  private val tid = "UA-7935029-3"
  private type PostArgs = Map[String, Seq[String]]

  // user makes a one-time or recurring donation
  def newDonation(user: User, amount: Cents, renew: Boolean): Unit = send(makeArgs(Map(
    "ec" -> "Conversion",
    "ea" -> "Donate",
    "el" -> (if (renew) "Recurring donation" else "One-time donation"),
    "ev" -> amount.usd.toInt,
    "uid" -> user.id)))

  // user makes a second one-time donation
  def reDonation(user: User, amount: Cents): Unit = send(makeArgs(Map(
    "ec" -> "Conversion",
    "ea" -> "Donate",
    "el" -> "Redonation",
    "ev" -> amount.usd.toInt,
    "uid" -> user.id)))

  // user makes a recurring donation after a one-time donoation
  def upgrade(user: User, amount: Cents): Unit = send(makeArgs(Map(
    "ec" -> "Conversion",
    "ea" -> "Donate",
    "el" -> "Upgrade",
    "ev" -> amount.usd.toInt,
    "uid" -> user.id)))

  def charge(charge: Charge, renew: Boolean): Unit = send(makeArgs(Map(
    "ti" -> charge.id,
    "t" -> "transaction",
    "in" -> (if (renew) "Recurring payment" else "One-time payment"),
    "ip" -> charge.cents.usd.value,
    "tr" -> charge.cents.usd.value,
    "cu" -> "USD",
    "iq" -> 1,
    "ic" -> (if (renew) "RecurringDonationPaymentSKU" else "OneTimeDonationPaymentSKU"),
    "iv" -> "Donation",
    "ta" -> (if (charge.isPayPal) "PayPal" else "Stripe"),
    "uid" -> charge.userId)))

  private def send(args: PostArgs): Unit =
    WS.url(url).post(args).effectFold(
      err => logger.warn(s"tracking $url $args", err), {
        case res if res.status == 200 =>
        case res                      => logger.warn(s"tracking $url $args ${res.status} ${res.body}")
      })

  private def makeArgs(args: Map[String, Any]): PostArgs =
    baseArgs ++ args.mapValues(v => Seq(v.toString))

  private def baseArgs: PostArgs = Map(
    "v" -> Seq("1"),
    "tid" -> Seq(tid),
    "cid" -> Seq(java.util.UUID.randomUUID.toString),
    "t" -> Seq("event"),
    "ds" -> Seq("backend"))
}
