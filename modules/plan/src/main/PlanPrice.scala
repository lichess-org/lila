package lila.plan

import cats.implicits._
import java.util.{ Currency, Locale }
import scala.concurrent.ExecutionContext

case class PlanPrices(locale: Locale, defaults: List[Money], min: Money, max: Money)

final class PlanPriceApi(currencyApi: CurrencyApi)(implicit ec: ExecutionContext) {

  import currencyApi.US

  val usdPrices = PlanPrices(
    locale = US,
    defaults = List(5, 10, 20, 50).map(usd => Money(usd, US)),
    min = Money(1, US),
    max = Money(10000, US)
  )

  def pricesFor(locale: Locale): Fu[PlanPrices] =
    if (locale == US) fuccess(usdPrices)
    else {
      for {
        defaults <- currencyApi.convertFromUsd(usdPrices.defaults.map(_.amount), locale)
        min      <- currencyApi.convert(usdPrices.min, locale)
        max      <- currencyApi.convert(usdPrices.max, locale)
      } yield (locale.some, defaults, min, max).mapN(PlanPrices.apply)
    }.dmap(_ | usdPrices)
}
