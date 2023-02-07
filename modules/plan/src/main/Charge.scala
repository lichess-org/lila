package lila.plan

import cats.syntax.all.*
import ornicar.scalalib.ThreadLocalRandom

import lila.user.User

case class Charge(
    _id: String, // random
    userId: Option[UserId],
    giftTo: Option[UserId] = none,
    stripe: Option[Charge.Stripe] = none,
    payPal: Option[Charge.PayPalLegacy] = none,
    payPalCheckout: Option[Charge.PayPalCheckout] = none,
    money: Money,
    usd: Usd,
    date: DateTime
):

  inline def id = _id

  def isPayPalLegacy   = payPal.nonEmpty
  def isPayPalCheckout = payPalCheckout.nonEmpty
  def isStripe         = stripe.nonEmpty

  def serviceName =
    if (isStripe) "stripe"
    else if (isPayPalLegacy) "paypal legacy"
    else if (isPayPalCheckout) "paypal checkout"
    else "???"

  def toGift = (userId, giftTo) mapN { Charge.Gift(_, _, date) }

object Charge:

  def make(
      userId: Option[UserId],
      giftTo: Option[UserId],
      stripe: Option[Charge.Stripe] = none,
      payPal: Option[Charge.PayPalLegacy] = none,
      payPalCheckout: Option[Charge.PayPalCheckout] = none,
      money: Money,
      usd: Usd
  ) =
    Charge(
      _id = ThreadLocalRandom nextString 8,
      userId = userId,
      giftTo = giftTo,
      stripe = stripe,
      payPal = payPal,
      payPalCheckout = payPalCheckout,
      money = money,
      usd = usd,
      date = nowDate
    )

  case class Stripe(
      chargeId: StripeChargeId,
      customerId: StripeCustomerId
  )

  case class PayPalLegacy(
      ip: Option[String],
      name: Option[String],
      email: Option[Patron.PayPalLegacy.Email],
      txnId: Option[String],
      subId: Option[Patron.PayPalLegacy.SubId]
  )

  case class PayPalCheckout(
      orderId: PayPalOrderId,
      payerId: PayPalPayerId,
      subscriptionId: Option[PayPalSubscriptionId]
  )

  case class Gift(from: UserId, to: UserId, date: DateTime)
