package lila.plan

import java.text.NumberFormat
import java.util.{ Currency, Locale }
import org.joda.time.DateTime
import play.api.i18n.Lang

import lila.user.User

case class ChargeId(value: String)       extends AnyVal
case class ClientId(value: String)       extends AnyVal
case class CustomerId(value: String)     extends AnyVal
case class SessionId(value: String)      extends AnyVal
case class SubscriptionId(value: String) extends AnyVal

case class Source(value: String) extends AnyVal

sealed abstract class Freq(val renew: Boolean)
object Freq {
  case object Monthly extends Freq(renew = true)
  case object Onetime extends Freq(renew = false)
}

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

case class Money(amount: BigDecimal, currency: Currency) {
  def display(locale: Locale): String = {
    val format = NumberFormat.getCurrencyInstance(locale)
    format setCurrency currency
    format format amount
  }
  def display(implicit lang: Lang): String = display(lang.locale)
  def currencyCode                         = currency.getCurrencyCode
  def code                                 = s"${currencyCode}_$amount"
  def toStripeAmount = StripeAmount {
    if (CurrencyApi.zeroDecimalCurrencies(currency)) amount.toInt else (amount * 100).toInt
  }
  override def toString = code
}

case class Usd(value: BigDecimal) extends AnyVal {
  def cents = (value * 100).toInt
}

case class Country(code: String) extends AnyVal

case class StripeSubscriptions(data: List[StripeSubscription])

case class StripeProducts(monthly: String, onetime: String, gift: String)

case class StripeItem(id: String, price: StripePrice)

case class StripePrice(product: String, unit_amount: StripeAmount, currency: Currency) {
  def amount = unit_amount
  def money  = unit_amount toMoney currency
}

case class NextUrls(cancel: String, success: String)

case class StripeSession(id: SessionId)
case class CreateStripeSession(
    customerId: CustomerId,
    checkout: Checkout,
    urls: NextUrls,
    giftTo: Option[User]
)

case class StripeSubscription(
    id: String,
    item: StripeItem,
    customer: CustomerId,
    cancel_at_period_end: Boolean,
    status: String,
    default_payment_method: Option[String]
) {
  def renew    = !cancel_at_period_end
  def isActive = status == "active"
}

case class StripeCustomer(
    id: CustomerId,
    email: Option[String],
    subscriptions: StripeSubscriptions
) {

  def firstSubscription = subscriptions.data.headOption
  def renew             = firstSubscription.??(_.renew)
}

case class StripeCharge(
    id: ChargeId,
    amount: StripeAmount,
    currency: Currency,
    customer: CustomerId,
    billing_details: Option[StripeCharge.BillingDetails]
) {
  def country = billing_details.flatMap(_.address).flatMap(_.country).map(Country)
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

case class StripeCompletedSession(customer: CustomerId, mode: String, metadata: Map[String, String]) {
  def freq   = if (mode == "subscription") Freq.Monthly else Freq.Onetime
  def giftTo = metadata get "gift"
}

case class StripeSetupIntent(payment_method: String)

case class StripeSessionWithIntent(setup_intent: StripeSetupIntent)
