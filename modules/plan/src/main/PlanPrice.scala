package lila.plan

import cats.implicits._
import java.util.{ Currency, Locale }
import scala.concurrent.ExecutionContext

case class PlanPricing(locale: Locale, suggestions: List[Money], min: Money, max: Money, lifetime: Money) {

  val default = suggestions.lift(1) orElse suggestions.headOption getOrElse min

  def currencyCode = min.currency.getCurrencyCode
}

final class PlanPriceApi(currencyApi: CurrencyApi)(implicit ec: ExecutionContext) {

  import currencyApi.US

  val usdPricing = PlanPricing(
    locale = US,
    suggestions = List(5, 10, 20, 50).map(usd => Money(usd, US)),
    min = Money(1, US),
    max = Money(10000, US),
    lifetime = Money(250, US)
  )

  def pricingFor(locale: Locale): Fu[PlanPricing] =
    if (locale == US) fuccess(usdPricing)
    else {
      for {
        allSuggestions <- usdPricing.suggestions.map(convertAndRound(_, locale)).sequenceFu.map(_.sequence)
        suggestions = allSuggestions.map(_.distinct)
        min      <- convertAndRound(usdPricing.min, locale)
        max      <- convertAndRound(usdPricing.max, locale)
        lifetime <- convertAndRound(usdPricing.lifetime, locale)
      } yield (locale.some, suggestions, min, max, lifetime).mapN(PlanPricing.apply)
    }.dmap(_ | usdPricing)

  private def convertAndRound(money: Money, to: Locale): Fu[Option[Money]] =
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
