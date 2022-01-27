package lila.plan

import cats.implicits._
import java.util.{ Currency, Locale }
import scala.concurrent.ExecutionContext

case class PlanPricing(suggestions: List[Money], min: Money, max: Money, lifetime: Money) {

  val default = suggestions.lift(1) orElse suggestions.headOption getOrElse min

  def currency     = min.currency
  def currencyCode = currency.getCurrencyCode

  def valid(money: Money): Boolean       = money.currency == currency && valid(money.amount)
  def valid(amount: BigDecimal): Boolean = min.amount <= amount && amount <= max.amount
}

final class PlanPricingApi(currencyApi: CurrencyApi)(implicit ec: ExecutionContext) {

  import currencyApi.{ EUR, USD }

  val usdPricing = PlanPricing(
    suggestions = List(5, 10, 20, 50).map(usd => Money(usd, USD)),
    min = Money(1, USD),
    max = Money(10000, USD),
    lifetime = Money(250, USD)
  )

  val eurPricing = PlanPricing(
    suggestions = List(5, 10, 20, 50).map(eur => Money(eur, EUR)),
    min = Money(1, EUR),
    max = Money(10000, EUR),
    lifetime = Money(200, EUR)
  )

  def pricingFor(currency: Currency): Fu[Option[PlanPricing]] =
    if (currency == USD) fuccess(usdPricing.some)
    else if (currency == EUR) fuccess(eurPricing.some)
    else
      for {
        allSuggestions <- usdPricing.suggestions.map(convertAndRound(_, currency)).sequenceFu.map(_.sequence)
        suggestions = allSuggestions.map(_.distinct)
        min      <- convertAndRound(usdPricing.min, currency)
        max      <- convertAndRound(usdPricing.max, currency)
        lifetime <- convertAndRound(usdPricing.lifetime, currency)
      } yield (suggestions, min, max, lifetime).mapN(PlanPricing)

  def pricingOrDefault(currency: Currency): Fu[PlanPricing] = pricingFor(currency).dmap(_ | usdPricing)

  def isLifetime(money: Money): Fu[Boolean] =
    pricingFor(money.currency) map {
      _.exists(_.lifetime.amount <= money.amount)
    }

  private def convertAndRound(money: Money, to: Currency): Fu[Option[Money]] =
    currencyApi.convert(money, to) map2 { case Money(amount, locale) =>
      Money(PlanPricingApi.nicelyRound(amount), locale)
    }
}

object PlanPricingApi {

  def nicelyRound(amount: BigDecimal): BigDecimal = {
    val double   = amount.toDouble
    val scale    = math.floor(math.log10(double));
    val fraction = if (scale > 1) 2d else 1d
    math.round(double * fraction * math.pow(10, -scale)) / fraction / math.pow(10, -scale)
  } atLeast 1

  import play.api.libs.json._
  val pricingWrites = OWrites[PlanPricing] { p =>
    Json.obj(
      "currency"    -> p.currencyCode,
      "min"         -> p.min.amount,
      "max"         -> p.max.amount,
      "lifetime"    -> p.lifetime.amount,
      "default"     -> p.default.amount,
      "suggestions" -> p.suggestions.map(_.amount)
    )
  }
}
