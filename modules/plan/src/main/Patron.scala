package lila.plan

case class Patron(
    _id: UserId,
    stripe: Option[Patron.Stripe] = none,
    payPal: Option[Patron.PayPalLegacy] = none,
    payPalCheckout: Option[Patron.PayPalCheckout] = none,
    free: Option[Patron.Free] = none,
    expiresAt: Option[Instant] = none,
    lifetime: Option[Boolean] = None,
    lastLevelUp: Option[Instant] = None
):

  inline def id = _id
  inline def userId = _id

  def canLevelUp = lastLevelUp.exists(_.isBefore(nowInstant.minusDays(25)))

  def levelUpIfPossible =
    copy(
      lastLevelUp = if canLevelUp then nowInstant.some else lastLevelUp.orElse(nowInstant.some)
    )

  def expireInOneMonth: Patron =
    copy(
      expiresAt = nowInstant.plusMonths(1).plusDays(1).some
    )

  def expireInOneMonth(cond: Boolean): Patron =
    if cond then expireInOneMonth
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

object Patron:

  case class Stripe(customerId: StripeCustomerId)

  case class PayPalCheckout(
      orderId: PayPalOrderId,
      payerId: PayPalPayerId,
      subscriptionId: Option[PayPalSubscriptionId]
  ):
    def renew = subscriptionId.isDefined

  case class PayPalLegacy(
      email: Option[PayPalLegacy.Email],
      subId: Option[PayPalLegacy.SubId],
      lastCharge: Instant
  ):
    def renew = subId.isDefined
  object PayPalLegacy:
    opaque type Email = String
    object Email extends OpaqueString[Email]
    opaque type SubId = String
    object SubId extends OpaqueString[SubId]

  case class Free(at: Instant, by: Option[UserId])
