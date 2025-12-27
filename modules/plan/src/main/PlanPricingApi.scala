package lila.plan

import java.util.Currency
import play.api.libs.json.*
import lila.memo.CacheApi
import lila.memo.CacheApi.buildAsyncTimeout

case class PlanPricing(
    suggestions: List[Money],
    min: Money,
    max: Money,
    lifetime: Money,
    giftMin: Money,
    feeRate: Double,
    feeFixed: Money
):

  val default = suggestions.lift(1).orElse(suggestions.headOption).getOrElse(min)

  def currency = min.currency
  def currencyCode = currency.getCurrencyCode

  def payPalSupportsCurrency = CurrencyApi.payPalCurrencies.contains(currency)
  def stripeSupportsCurrency = CurrencyApi.stripeCurrencies.contains(currency)

  def valid(money: Money, isGift: Boolean): Boolean =
    money.currency == currency && valid(money.amount, isGift)
  private def valid(amount: BigDecimal, isGift: Boolean): Boolean =
    (if isGift then giftMin else min).amount <= amount && amount <= max.amount

final class PlanPricingApi(currencyApi: CurrencyApi, cacheApi: CacheApi)(using Executor, Scheduler):

  import currencyApi.{ EUR, USD }

  val usdPricing = PlanPricing(
    suggestions = List(5, 10, 20, 50).map(usd => Money(usd, USD)),
    min = Money(1, USD),
    max = Money(10000, USD),
    lifetime = Money(250, USD),
    giftMin = Money(2, USD),
    feeRate = PlanPricingApi.FeeRate,
    feeFixed = Money(0.35, USD)
  )

  val eurPricing = PlanPricing(
    suggestions = List(5, 10, 20, 50).map(eur => Money(eur, EUR)),
    min = Money(1, EUR),
    max = Money(10000, EUR),
    lifetime = Money(250, EUR),
    giftMin = Money(2, EUR),
    feeRate = PlanPricingApi.FeeRate,
    feeFixed = Money(0.35, EUR)
  )

  def pricingFor(currency: Currency): Fu[Option[PlanPricing]] =
    if currency == USD then fuccess(usdPricing.some)
    else if currency == EUR then fuccess(eurPricing.some)
    else
      for
        allSuggestions <- usdPricing.suggestions.parallel(convertAndRound(_, currency))
        min <- convertAndRound(usdPricing.min, currency)
        max <- convertAndRound(usdPricing.max, currency)
        lifetime <- convertAndRound(usdPricing.lifetime, currency)
        giftMin <- convertAndRound(usdPricing.giftMin, currency)
        feeFixed <- convertAndRound(usdPricing.feeFixed, currency, nice = false)
      yield (allSuggestions.sequence, min, max, lifetime, giftMin, feeFixed).mapN {
        (sugs, min, max, life, gift, fee) =>
          PlanPricing(sugs, min, max, life, gift, PlanPricingApi.FeeRate, fee)
      }

  def pricingOrDefault(currency: Currency): Fu[PlanPricing] = pricingFor(currency).dmap(_ | usdPricing)

  def isLifetime(money: Money): Fu[Boolean] =
    pricingFor(money.currency).map:
      _.exists(_.lifetime.amount <= money.amount)

  object stripePricesAsJson:
    private val placeholder = "{{myCurrency}}"
    private val cache = cacheApi.unit[JsonStr]:
      _.refreshAfterWrite(1.hour).buildAsyncTimeout(20.seconds): _ =>
        import PlanPricingApi.pricingWrites
        CurrencyApi.stripeCurrencyList
          .sequentially(pricingFor)
          .map: list =>
            Json.obj(
              "stripe" -> list.flatten,
              "myCurrency" -> placeholder
            )
          .map(Json.stringify)
          .dmap(JsonStr.apply)
    def apply(currency: Currency): Fu[JsonStr] =
      cache.get({}).map(_.map(_.replace(placeholder, currency.getCurrencyCode)))

  private def convertAndRound(money: Money, to: Currency, nice: Boolean = true): Fu[Option[Money]] =
    currencyApi.convert(money, to).map2 { m =>
      val amount =
        if nice then PlanPricingApi.nicelyRound(m.amount)
        else
          val scale = if CurrencyApi.zeroDecimalCurrencies contains to then 0 else 2
          m.amount.setScale(scale, BigDecimal.RoundingMode.HALF_UP)

      m.copy(amount = amount)
    }

object PlanPricingApi:

  private val FeeRate = 0.04 // 4%

  def nicelyRound(amount: BigDecimal): BigDecimal = {
    val double = amount.toDouble
    val scale = math.floor(math.log10(double))
    val fraction = if scale > 1 then 2d else 1d
    val nice = math.round(double * fraction * math.pow(10, -scale)) / fraction / math.pow(10, -scale)
    math.round(nice * 10_000) / 10_000
  }.atLeast(1)

  given pricingWrites: OWrites[PlanPricing] = OWrites: p =>
    Json.obj(
      "currency" -> p.currencyCode,
      "min" -> p.min.amount,
      "max" -> p.max.amount,
      "lifetime" -> p.lifetime.amount,
      "giftMin" -> p.giftMin.amount,
      "default" -> p.default.amount,
      "suggestions" -> p.suggestions.map(_.amount),
      "feeRate" -> p.feeRate,
      "feeFixed" -> p.feeFixed.amount
    )
