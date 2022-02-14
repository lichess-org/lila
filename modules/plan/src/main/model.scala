package lila.plan

import java.text.NumberFormat
import java.util.{ Currency, Locale }
import org.joda.time.DateTime
import play.api.i18n.Lang

import lila.user.User

case class Source(value: String) extends AnyVal

sealed abstract class Freq(val renew: Boolean)
object Freq {
  case object Monthly extends Freq(renew = true)
  case object Onetime extends Freq(renew = false)
}

case class Money(amount: BigDecimal, currency: Currency) {
  def display(locale: Locale): String = {
    val format = NumberFormat.getCurrencyInstance(locale)
    format setCurrency currency
    format format amount
  }
  def display(implicit lang: Lang): String = display(lang.locale)
  def currencyCode                         = currency.getCurrencyCode
  def code                                 = s"${currencyCode}_$amount"
  override def toString                    = code
}

case class Usd(value: BigDecimal) extends AnyVal {
  def cents = (value * 100).toInt
}

case class Country(code: String) extends AnyVal

case class NextUrls(cancel: String, success: String)

// stripe model

case class StripeChargeId(value: String)       extends AnyVal
case class StripeCustomerId(value: String)     extends AnyVal
case class StripeSessionId(value: String)      extends AnyVal
case class StripeSubscriptionId(value: String) extends AnyVal

// /!\ In smallest currency unit /!\
// https://stripe.com/docs/currencies#zero-decimal
case class StripeAmount(smallestCurrencyUnit: Int) extends AnyVal {
  def toMoney(currency: Currency) =
    Money(
      if (CurrencyApi zeroDecimalCurrencies currency) smallestCurrencyUnit
      else BigDecimal(smallestCurrencyUnit) / 100,
      currency
    )
}
object StripeAmount {
  def apply(money: Money): StripeAmount = StripeAmount {
    if (CurrencyApi.zeroDecimalCurrencies(money.currency)) money.amount.toInt else (money.amount * 100).toInt
  }
}

case class StripeSubscriptions(data: List[StripeSubscription])

case class StripeProducts(monthly: String, onetime: String, gift: String)

case class StripeItem(id: String, price: StripePrice)

case class StripePrice(product: String, unit_amount: StripeAmount, currency: Currency) {
  def amount = unit_amount
  def money  = unit_amount toMoney currency
}

case class StripeSession(id: StripeSessionId)
case class CreateStripeSession(
    customerId: StripeCustomerId,
    checkout: PlanCheckout,
    urls: NextUrls,
    giftTo: Option[User],
    isLifetime: Boolean
)

case class StripeSubscription(
    id: String,
    item: StripeItem,
    customer: StripeCustomerId,
    cancel_at_period_end: Boolean,
    status: String,
    default_payment_method: Option[String]
) {
  def renew    = !cancel_at_period_end
  def isActive = status == "active"
}

case class StripeCustomer(
    id: StripeCustomerId,
    email: Option[String],
    subscriptions: StripeSubscriptions
) {

  def firstSubscription = subscriptions.data.headOption
  def renew             = firstSubscription.??(_.renew)
}

case class StripeCharge(
    id: StripeChargeId,
    amount: StripeAmount,
    currency: Currency,
    customer: StripeCustomerId,
    billing_details: Option[StripeCharge.BillingDetails],
    metadata: Map[String, String]
) {
  def country                 = billing_details.flatMap(_.address).flatMap(_.country).map(Country)
  def giftTo: Option[User.ID] = metadata get "giftTo"
}

object StripeCharge {
  case class Address(country: Option[String])
  case class BillingDetails(address: Option[Address])
}

case class StripeInvoice(
    id: Option[String],
    amount_due: StripeAmount,
    currency: Currency,
    created: Long,
    paid: Boolean
) {
  def money    = amount_due toMoney currency
  def dateTime = new DateTime(created * 1000)
}

case class StripePaymentMethod(card: Option[StripeCard])

case class StripeCard(brand: String, last4: String, exp_year: Int, exp_month: Int)

case class StripeCompletedSession(
    customer: StripeCustomerId,
    mode: String,
    metadata: Map[String, String],
    amount_total: StripeAmount,
    currency: Currency
) {
  def freq                    = if (mode == "subscription") Freq.Monthly else Freq.Onetime
  def money                   = amount_total toMoney currency
  def giftTo: Option[User.ID] = metadata get "giftTo"
}

case class StripeSetupIntent(payment_method: String)

case class StripeSessionWithIntent(setup_intent: StripeSetupIntent)

// payPal model

case class PayPalAmount(value: BigDecimal, currency_code: Currency) {
  def money = Money(value, currency_code)
}
case class PayPalOrderId(value: String) extends AnyVal with StringValue
case class PayPalOrder(
    id: PayPalOrderId,
    intent: String,
    status: String,
    purchase_units: List[PayPalPurchaseUnit],
    payer: PayPalPayer
) {
  val (userId, giftTo) = purchase_units.headOption.flatMap(_.custom_id).??(_.trim) match {
    case s"$userId $giftTo" => (userId.some, giftTo.some)
    case s"$userId"         => (userId.some, none)
    case _                  => (none, none)
  }
  def isApproved        = status == "APPROVED"
  def isApprovedCapture = isApproved && intent == "CAPTURE"
  def capturedMoney     = isApprovedCapture ?? purchase_units.headOption.map(_.amount.money)
  def country           = payer.address.flatMap(_.country_code)
}
case class CreatePayPalOrder(
    checkout: PlanCheckout,
    user: User,
    giftTo: Option[User],
    isLifetime: Boolean
) {
  def makeCustomId = giftTo.fold(user.id) { g => s"${user.id} ${g.id}" }
}
case class PayPalOrderCreated(id: PayPalOrderId)
case class PayPalPurchaseUnit(amount: PayPalAmount, custom_id: Option[String])
case class PayPalPayerId(value: String) extends AnyVal with StringValue
case class PayPalPayer(payer_id: PayPalPayerId, address: Option[PayPalAddress]) {
  def id = payer_id
}
case class PayPalAddress(country_code: Option[Country])
