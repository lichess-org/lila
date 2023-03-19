package lila.plan

import cats.syntax.all.*
import ornicar.scalalib.ThreadLocalRandom

import lila.user.User
import org.joda.time.DateTime

case class Charge(
    _id: String, // random
    userId: Option[UserId],
    giftTo: Option[UserId] = none,
    stripe: Option[Charge.Stripe] = none,
    payPal: Option[Charge.PayPalLegacy] = none,
    payPalCheckout: Option[Patron.PayPalCheckout] = none,
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

  def copyAsNew = copy(_id = Charge.makeId, date = DateTime.now)

object Charge:

  private def makeId = ThreadLocalRandom nextString 8

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

  case class Gift(from: UserId, to: UserId, date: DateTime)
