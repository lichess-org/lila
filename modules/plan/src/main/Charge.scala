package lila.plan

import org.joda.time.DateTime
import ornicar.scalalib.Random

case class Charge(
    _id: String, // random
    userId: Option[String],
    stripe: Option[Charge.Stripe] = none,
    payPal: Option[Charge.PayPal] = none,
    cents: Cents,
    date: DateTime) {

  def serviceName =
    if (stripe.nonEmpty) "stripe"
    else if (payPal.nonEmpty) "paypal"
    else "???"
}

object Charge {

  def make(
    userId: Option[String],
    stripe: Option[Charge.Stripe] = none,
    payPal: Option[Charge.PayPal] = none,
    cents: Cents) = Charge(
    _id = Random nextStringUppercase 8,
    userId = userId,
    stripe = stripe,
    payPal = payPal,
    cents = cents,
    date = DateTime.now)

  case class Stripe(chargeId: ChargeId, customerId: CustomerId)
  case class PayPal(email: Option[String], subId: Option[String])
}
