package lila.plan

import play.api.i18n.Lang
import play.api.libs.json.{ JsArray, JsObject }

import java.text.NumberFormat
import java.util.{ Currency, Locale }

import lila.core.net.IpAddress

case class Source(value: String) extends AnyVal

enum Freq(val renew: Boolean):
  case Monthly extends Freq(renew = true)
  case Onetime extends Freq(renew = false)

case class Money(amount: BigDecimal, currency: Currency):
  def display(locale: Locale): String =
    val format = NumberFormat.getCurrencyInstance(locale)
    format.setCurrency(currency)
    val digits = math.max(0, currency.getDefaultFractionDigits)
    format.setMinimumFractionDigits(digits)
    format.setMaximumFractionDigits(digits)
    format.format(amount)
  def display(using lang: Lang): String = display(lang.locale)
  def currencyCode = currency.getCurrencyCode
  def code = s"${currencyCode}_$amount"
  override def toString = code

opaque type Usd = BigDecimal
object Usd extends TotalWrapper[Usd, BigDecimal]:
  extension (e: Usd) def cents = (e * 100).toInt

opaque type Country = String
object Country extends OpaqueString[Country]

case class NextUrls(cancel: Url, success: Url)

case class ProductIds(monthly: String, onetime: String, gift: String)

// stripe model

opaque type StripeChargeId = String
object StripeChargeId extends OpaqueString[StripeChargeId]
opaque type StripeCustomerId = String
object StripeCustomerId extends OpaqueString[StripeCustomerId]
opaque type StripeSessionId = String
object StripeSessionId extends OpaqueString[StripeSessionId]
opaque type StripeSubscriptionId = String
object StripeSubscriptionId extends OpaqueString[StripeSubscriptionId]

// /!\ In smallest currency unit /!\
// https://stripe.com/docs/currencies#zero-decimal
opaque type StripeAmount = Int
object StripeAmount extends OpaqueInt[lila.plan.StripeAmount]:
  extension (e: StripeAmount)
    def toMoney(currency: Currency) =
      Money(
        if CurrencyApi.zeroDecimalCurrencies(currency) then e
        else BigDecimal(e) / 100,
        currency
      )
  def apply(money: Money): StripeAmount = StripeAmount:
    if CurrencyApi.zeroDecimalCurrencies(money.currency) then money.amount.toInt
    else (money.amount * 100).toInt

case class StripeSubscriptions(data: List[StripeSubscription])

case class StripeItem(id: String, price: StripePrice)

case class StripePrice(product: String, unit_amount: StripeAmount, currency: Currency):
  def amount = unit_amount
  def money = unit_amount.toMoney(currency)

case class StripeSession(id: StripeSessionId, payment_intent: Option[StripePaymentIntent]):
  def clientSecret = payment_intent.map(_.client_secret)

case class StripePaymentIntent(client_secret: String)

trait StripeSessionData:
  def customerId: StripeCustomerId
  def currency: Currency
  def ipOption: Option[IpAddress]

case class CreateStripeSession(
    customerId: StripeCustomerId,
    checkout: PlanCheckout,
    urls: NextUrls,
    giftTo: Option[User],
    isLifetime: Boolean,
    ip: IpAddress
) extends StripeSessionData:
  def currency = checkout.money.currency
  def ipOption = ip.some

case class StripeSubscription(
    id: String,
    item: StripeItem,
    customer: StripeCustomerId,
    cancel_at_period_end: Boolean,
    status: String,
    default_payment_method: Option[String],
    ip: Option[IpAddress]
) extends StripeSessionData:
  def renew = !cancel_at_period_end
  def isActive = status == "active"
  def customerId = customer
  def currency = item.price.currency
  def ipOption = ip

case class StripeCustomer(
    id: StripeCustomerId,
    email: Option[String],
    subscriptions: StripeSubscriptions
):

  def firstSubscription = subscriptions.data.headOption
  def renew = firstSubscription.so(_.renew)

case class StripeCharge(
    id: StripeChargeId,
    amount: StripeAmount,
    currency: Currency,
    customer: StripeCustomerId,
    billing_details: Option[StripeCharge.BillingDetails],
    metadata: Map[String, String]
):
  def country = billing_details.flatMap(_.address).flatMap(_.country)
  def giftTo: Option[UserId] = UserId.from(metadata.get("giftTo"))
  def ip: Option[IpAddress] = metadata.get("ipAddress").flatMap(IpAddress.from)

object StripeCharge:
  case class Address(country: Option[Country])
  case class BillingDetails(address: Option[Address])

case class StripeInvoice(
    id: Option[String],
    amount_due: StripeAmount,
    currency: Currency,
    created: Long,
    paid: Boolean
):
  def money = amount_due.toMoney(currency)
  def dateTime = millisToInstant(created * 1000)

case class StripePaymentMethod(card: Option[StripeCard])

case class StripeCard(brand: String, last4: String, exp_year: Int, exp_month: Int)

case class StripeCompletedSession(
    customer: StripeCustomerId,
    mode: String,
    metadata: Map[String, String],
    amount_total: StripeAmount,
    currency: Currency
):
  def freq = if mode == "subscription" then Freq.Monthly else Freq.Onetime
  def money = amount_total.toMoney(currency)
  def giftTo: Option[UserId] = UserId.from(metadata.get("giftTo"))

case class StripeSetupIntent(payment_method: String)

case class StripeSessionWithIntent(setup_intent: StripeSetupIntent)

enum StripeMode:
  case setup, payment, subscription

opaque type StripeCanUse = Boolean
object StripeCanUse extends YesNo[StripeCanUse]

// payPal model

case class PayPalAmount(value: BigDecimal, currency_code: Currency):
  def money = Money(value, currency_code)
case class PayPalOrderId(value: String) extends AnyVal with StringValue
case class PayPalSubscriptionId(value: String) extends AnyVal with StringValue
case class PayPalOrder(
    id: PayPalOrderId,
    intent: String,
    status: String,
    purchase_units: List[PayPalPurchaseUnit],
    payer: PayPalPayer
):
  val (userId, giftTo) = purchase_units.headOption.flatMap(_.custom_id).so(_.trim) match
    case s"$userId $giftTo" => (UserId(userId).some, UserId(giftTo).some)
    case s"$userId" => (UserId(userId).some, none)
    case _ => (none, none)
  def isCompleted = status == "COMPLETED"
  def isCompletedCapture = isCompleted && intent == "CAPTURE"
  def capturedMoney = isCompletedCapture.so(purchase_units.headOption.map(_.amount.money))
  def country = payer.address.flatMap(_.country_code)
case class PayPalPayment(amount: PayPalAmount)
case class PayPalBillingInfo(last_payment: PayPalPayment, next_billing_time: Instant)
case class PayPalSubscription(
    id: PayPalSubscriptionId,
    status: String,
    subscriber: PayPalPayer,
    billing_info: PayPalBillingInfo
):
  def country = subscriber.address.flatMap(_.country_code)
  def capturedMoney = billing_info.last_payment.amount.money
  def nextChargeAt = billing_info.next_billing_time
  def isActive = status == "ACTIVE"
case class CreatePayPalOrder(
    checkout: PlanCheckout,
    user: User,
    giftTo: Option[User],
    isLifetime: Boolean
):
  def makeCustomId = giftTo.fold(user.id.value) { g => s"${user.id} ${g.id}" }
case class PayPalOrderCreated(id: PayPalOrderId)
case class PayPalSubscriptionCreated(id: PayPalSubscriptionId)
case class PayPalPurchaseUnit(amount: PayPalAmount, custom_id: Option[String])
case class PayPalPayerId(value: String) extends AnyVal with StringValue
case class PayPalPayer(payer_id: PayPalPayerId, address: Option[PayPalAddress]):
  def id = payer_id
case class PayPalAddress(country_code: Option[Country])

case class PayPalEventId(value: String) extends AnyVal with StringValue
case class PayPalEvent(id: PayPalEventId, event_type: String, resource_type: String, resource: JsObject):
  def tpe = event_type
  def resourceTpe = resource_type
  def resourceId = resource.str("id")

case class PayPalPlanId(value: String) extends AnyVal with StringValue
case class PayPalPlan(id: PayPalPlanId, name: String, status: String, billing_cycles: JsArray):
  import JsonHandlers.payPal.given
  def active = status == "ACTIVE"
  val currency = for
    cycle <- billing_cycles.value.headOption
    pricing <- cycle.obj("pricing_scheme")
    price <- pricing.get[PayPalAmount]("fixed_price")
  yield price.money.currency
case class PayPalTransactionId(value: String) extends AnyVal with StringValue
case class PayPalCapture(
    id: PayPalTransactionId,
    amount: PayPalAmount,
    custom_id: String,
    status: String,
    billing_agreement_id: Option[PayPalSubscriptionId]
):
  def isCompleted = status.toUpperCase == "COMPLETED"
  def capturedMoney = isCompleted.option(amount.money)
  def userId = UserId(custom_id)
  def subscriptionId = billing_agreement_id
case class PayPalSaleAmount(total: BigDecimal, currency: Currency):
  def amount = PayPalAmount(total, currency)
case class PayPalSale(
    id: PayPalTransactionId,
    amount: PayPalSaleAmount,
    custom: String,
    state: String,
    billing_agreement_id: Option[PayPalSubscriptionId]
):
  def toCapture = PayPalCapture(id, amount.amount, custom_id = custom, status = state, billing_agreement_id)
