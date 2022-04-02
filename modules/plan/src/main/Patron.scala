package lila.plan

import org.joda.time.DateTime

import lila.user.User

case class Patron(
    _id: Patron.UserId,
    stripe: Option[Patron.Stripe] = none,
    payPal: Option[Patron.PayPalLegacy] = none,
    payPalCheckout: Option[Patron.PayPalCheckout] = none,
    free: Option[Patron.Free] = none,
    expiresAt: Option[DateTime] = none,
    lifetime: Option[Boolean] = None,
    lastLevelUp: Option[DateTime] = None
) {

  def id = _id

  def userId = _id.value

  def canLevelUp = lastLevelUp.exists(_.isBefore(DateTime.now.minusDays(25)))

  def levelUpIfPossible =
    copy(
      lastLevelUp = if (canLevelUp) Some(DateTime.now) else lastLevelUp orElse Some(DateTime.now)
    )

  def expireInOneMonth: Patron =
    copy(
      expiresAt = DateTime.now.plusMonths(1).plusDays(1).some
    )

  def expireInOneMonth(cond: Boolean): Patron =
    if (cond) expireInOneMonth
    else copy(expiresAt = none)

  def removeStripe =
    copy(
      stripe = none,
      expiresAt = none
    )

  def removePayPalCheckout =
    copy(
      payPalCheckout = none,
      expiresAt = none
    )

  def removePayPal =
    copy(
      payPal = none,
      expiresAt = none
    )

  def isDefined = stripe.isDefined || payPal.isDefined

  def isLifetime = ~lifetime
}

object Patron {

  case class UserId(value: String) extends AnyVal

  case class Stripe(customerId: StripeCustomerId)
  case class PayPalCheckout(payerId: PayPalPayerId, subscriptionId: Option[PayPalSubscriptionId]) {
    def renew = subscriptionId.isDefined
  }

  case class PayPalLegacy(
      email: Option[PayPalLegacy.Email],
      subId: Option[PayPalLegacy.SubId],
      lastCharge: DateTime
  ) {
    def renew = subId.isDefined
  }
  object PayPalLegacy {
    case class Email(value: String) extends AnyVal
    case class SubId(value: String) extends AnyVal
  }

  case class Free(at: DateTime, by: Option[User.ID])
}
