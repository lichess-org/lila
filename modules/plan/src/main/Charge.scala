package lila.plan

import org.joda.time.DateTime

case class Charge(
    _id: String, // random
    userId: Option[String],
    stripe: Option[Charge.Stripe] = none,
    payPal: Option[Charge.PayPal] = none,
    cents: Cents,
    date: DateTime
) {

  def id = _id

  def isPayPal = payPal.nonEmpty
  def isStripe = stripe.nonEmpty

  def serviceName =
    if (isStripe) "stripe"
    else if (isPayPal) "paypal"
    else "???"

  def lifetimeWorthy = cents >= Cents.lifetime
}

object Charge {

  def make(
      userId: Option[String],
      stripe: Option[Charge.Stripe] = none,
      payPal: Option[Charge.PayPal] = none,
      cents: Cents
  ) =
    Charge(
      _id = lila.common.ThreadLocalRandom nextString 8,
      userId = userId,
      stripe = stripe,
      payPal = payPal,
      cents = cents,
      date = DateTime.now
    )

  case class Stripe(
      chargeId: ChargeId,
      customerId: CustomerId
  )

  case class PayPal(
      ip: Option[String],
      name: Option[String],
      email: Option[String],
      txnId: Option[String],
      subId: Option[String]
  )
}
