package lila.plan

import scalalib.ThreadLocalRandom

case class Charge(
    _id: String, // random
    userId: Option[UserId],
    giftTo: Option[UserId] = none,
    stripe: Option[Charge.Stripe] = none,
    payPal: Option[Charge.PayPalLegacy] = none,
    payPalCheckout: Option[Patron.PayPalCheckout] = none,
    money: Money,
    usd: Usd,
    date: Instant
):

  inline def id = _id

  def isPayPalLegacy = payPal.nonEmpty
  def isPayPalCheckout = payPalCheckout.nonEmpty
  def isStripe = stripe.nonEmpty

  def serviceName =
    if isStripe then "stripe"
    else if isPayPalLegacy then "paypal legacy"
    else if isPayPalCheckout then "paypal checkout"
    else "???"

  def toGift = (userId, giftTo).mapN { Charge.Gift(_, _, date) }

  def copyAsNew = copy(_id = Charge.makeId, date = nowInstant)

object Charge:

  private def makeId = ThreadLocalRandom.nextString(8)

  def make(
      userId: Option[UserId],
      giftTo: Option[UserId],
      stripe: Option[Charge.Stripe] = none,
      payPal: Option[Charge.PayPalLegacy] = none,
      payPalCheckout: Option[Patron.PayPalCheckout] = none,
      money: Money,
      usd: Usd
  ) =
    Charge(
      _id = makeId,
      userId = userId,
      giftTo = giftTo,
      stripe = stripe,
      payPal = payPal,
      payPalCheckout = payPalCheckout,
      money = money,
      usd = usd,
      date = nowInstant
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

  case class Gift(from: UserId, to: UserId, date: Instant)
