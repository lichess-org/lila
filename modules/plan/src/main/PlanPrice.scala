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

final class PlanPriceApi(currencyApi: CurrencyApi)(implicit ec: ExecutionContext) {

  import currencyApi.USD

  val usdPricing = PlanPricing(
    suggestions = List(5, 10, 20, 50).map(usd => Money(usd, USD)),
    min = Money(1, USD),
    max = Money(10000, USD),
    lifetime = Money(250, USD)
  )

  def pricingFor(currency: Currency): Fu[Option[PlanPricing]] =
    if (currency == USD) fuccess(usdPricing.some)
    else {
      for {
        allSuggestions <- usdPricing.suggestions.map(convertAndRound(_, currency)).sequenceFu.map(_.sequence)
        suggestions = allSuggestions.map(_.distinct)
        min      <- convertAndRound(usdPricing.min, currency)
        max      <- convertAndRound(usdPricing.max, currency)
        lifetime <- convertAndRound(usdPricing.lifetime, currency)
      } yield (suggestions, min, max, lifetime).mapN(PlanPricing.apply)
    }

  def pricingOrDefault(currency: Currency): Fu[PlanPricing] = pricingFor(currency).dmap(_ | usdPricing)

  def isLifetime(money: Money): Fu[Boolean] =
    pricingFor(money.currency) map {
      _.exists(_.lifetime.amount <= money.amount)
    }

  private def convertAndRound(money: Money, to: Currency): Fu[Option[Money]] =
    currencyApi.convert(money, to) map2 { case Money(amount, locale) =>
      Money(PlanPriceApi.nicelyRound(amount), locale)
    }
}

object PlanPriceApi {

  // round to closest number in 1-2-5 series
  private def nicelyRound(amount: BigDecimal): BigDecimal =
    if (amount <= 0) amount // ?
    else {
      val scale     = math.floor(math.log10(amount.toDouble))
      val leadDigit = math.round((amount / math.pow(10, scale)).toDouble)
      val multiplier =
        if (leadDigit == 1) 1
        else if (leadDigit <= 3) 2
        else if (leadDigit <= 7) 5
        else 10
      math.pow(10, scale) * multiplier
    }

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
