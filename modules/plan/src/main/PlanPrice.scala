package lila.plan

import cats.implicits._
import java.util.{ Currency, Locale }
import scala.concurrent.ExecutionContext

case class PlanPrices(locale: Locale, suggestions: List[Money], min: Money, max: Money, lifetime: Money)

final class PlanPriceApi(currencyApi: CurrencyApi)(implicit ec: ExecutionContext) {

  import currencyApi.US

  val usdPrices = PlanPrices(
    locale = US,
    suggestions = List(5, 10, 20, 50).map(usd => Money(usd, US)),
    min = Money(1, US),
    max = Money(10000, US),
    lifetime = Money(250, US)
  )

  def pricesFor(locale: Locale): Fu[PlanPrices] =
    if (locale == US) fuccess(usdPrices)
    else {
      for {
        defaults <- usdPrices.suggestions.map(convertAndRound(_, locale)).sequenceFu.map(_.sequence)
        min      <- convertAndRound(usdPrices.min, locale)
        max      <- convertAndRound(usdPrices.max, locale)
        lifetime <- convertAndRound(usdPrices.lifetime, locale)
      } yield (locale.some, defaults, min, max, lifetime).mapN(PlanPrices.apply)
    }.dmap(_ | usdPrices)

  private def convertAndRound(money: Money, to: Locale): Fu[Option[Money]] =
    currencyApi.convert(money, to) map2 { case Money(amount, locale) =>
      Money(PlanPriceApi.nicelyRound(amount), locale)
    }
}

private object PlanPriceApi {

  def nicelyRound(amount: BigDecimal): BigDecimal =
    if (amount <= 0) amount // ?
    else {
      val scale     = math.floor(math.log10(amount.toDouble))
      val leadDigit = (amount / math.pow(10, scale)).toInt
      val multiplier =
        if (leadDigit == 1) 1
        else if (leadDigit <= 3) 2
        else if (leadDigit <= 7) 5
        else 10
      scale * multiplier
    }
}
