package lila.plan

import play.api.ConfigLoader
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import java.util.{ Currency, Locale }
import scala.util.Try

import lila.common.autoconfig.AutoConfig
import lila.common.config.given
import lila.core.config.Secret
import lila.db.dsl.mapHandler
import lila.ui.Context

case class CurrencyWithRate(currency: Currency, rate: Double)

final class CurrencyApi(
    ws: StandaloneWSClient,
    mongoCache: lila.memo.MongoCache.Api,
    config: CurrencyApi.Config
)(using ec: Executor, mode: play.api.Mode):

  import CurrencyApi.*

  private val baseUrl = "https://openexchangerates.org/api"

  private val ratesCache = mongoCache.unit[Map[String, Double]](
    "currency:rates",
    if mode.isProd then 120.minutes // i.e. 377/month, under the 1000/month limit of free OER plan
    else 1.day
  ): loader =>
    _.refreshAfterWrite(121.minutes).buildAsyncFuture:
      loader: _ =>
        ws.url(s"$baseUrl/latest.json")
          .withQueryStringParameters("app_id" -> config.appId.value)
          .get()
          .dmap: res =>
            (res.body[JsValue] \ "rates").validate[Map[String, Double]].asOpt match
              case None =>
                logger.error(s"Currency rates unavailable! ${res.status} $baseUrl")
                Map("USD" -> 1d)
              case Some(rates) => rates.filterValues(0 <)

  def convert(money: Money, currency: Currency): Fu[Option[Money]] =
    ratesCache.get {}.map { rates =>
      for
        fromRate <- rates.get(money.currencyCode)
        toRate <- rates.get(currency.getCurrencyCode)
      yield Money(money.amount / fromRate * toRate, currency)
    }

  def toUsd(money: Money): Fu[Option[Usd]] =
    ratesCache.get {}.map { rates =>
      rates.get(money.currencyCode).map { fromRate =>
        Usd(money.amount / fromRate)
      }
    }

  val USD = Currency.getInstance("USD")
  val EUR = Currency.getInstance("EUR")

  def guessCurrency(requested: Option[String], ipCountry: => Option[String])(using ctx: Context): Currency =
    requested
      .flatMap(lila.plan.CurrencyApi.currencyOption)
      .getOrElse:
        ipCountry
          .flatMap { code => Try(new Locale.Builder().setRegion(code).build()).toOption }
          .flatMap(currencyOption)
          .orElse(currencyOption(ctx.lang.locale))
          .getOrElse(USD)

object CurrencyApi:

  case class Config(appId: Secret)
  given ConfigLoader[Config] = AutoConfig.loader

  private lazy val acceptableCurrencies: Set[Currency] = payPalCurrencies.concat(stripeCurrencies)

  lazy val currencyList: List[Currency] = acceptableCurrencies.toList.sortBy(_.getCurrencyCode)

  lazy val stripeCurrencyList: List[Currency] = stripeCurrencies.toList.sortBy(_.getCurrencyCode)

  def currencyOption(code: String) = anyCurrencyOption(code).filter(acceptableCurrencies.contains)
  def currencyOption(locale: Locale) =
    Try(Currency.getInstance(locale)).toOption.filter(acceptableCurrencies.contains)

  val zeroDecimalCurrencies: Set[Currency] = Set(
    "BIF",
    "CLP",
    "DJF",
    "GNF",
    "JPY",
    "KMF",
    "KRW",
    "MGA",
    "PYG",
    "RWF",
    "UGX",
    "VND",
    "VUV",
    "XAF",
    "XOF",
    "XPF"
  ).flatMap(anyCurrencyOption)

  // https://developer.paypal.com/docs/reports/reference/paypal-supported-currencies/
  val payPalCurrencies: Set[Currency] = Set(
    "AUD",
    "BRL",
    "CAD",
    // "CNY",
    "CZK",
    "DKK",
    "EUR",
    "HKD",
    "HUF",
    "ILS",
    "JPY",
    // "MYR",
    "MXN",
    "TWD",
    "NZD",
    "NOK",
    "PHP",
    "PLN",
    "GBP",
    "RUB",
    "SGD",
    "SEK",
    "CHF",
    "THB",
    "USD"
  ).flatMap(anyCurrencyOption)

  val stripeCurrencies: Set[Currency] = Set(
    "USD",
    "AED",
    "AFN",
    "ALL",
    "AMD",
    "ANG",
    "AOA",
    "ARS",
    "AUD",
    "AWG",
    "AZN",
    "BAM",
    "BBD",
    "BDT",
    "BGN",
    "BIF",
    "BMD",
    "BND",
    "BOB",
    "BRL",
    "BSD",
    "BWP",
    "BZD",
    "CAD",
    "CDF",
    "CHF",
    "CLP",
    "CNY",
    "COP",
    "CRC",
    "CVE",
    "CZK",
    "DJF",
    "DKK",
    "DOP",
    "DZD",
    "EGP",
    "ETB",
    "EUR",
    "FJD",
    "FKP",
    "GBP",
    "GEL",
    "GIP",
    "GMD",
    "GNF",
    "GTQ",
    "GYD",
    "HKD",
    "HNL",
    "HRK",
    "HTG",
    "HUF",
    "IDR",
    "ILS",
    "INR",
    "ISK",
    "JMD",
    "JPY",
    "KES",
    "KGS",
    "KHR",
    "KMF",
    "KRW",
    "KYD",
    "KZT",
    "LAK",
    "LBP",
    "LKR",
    "LRD",
    "LSL",
    "MAD",
    "MDL",
    "MGA",
    "MKD",
    "MMK",
    "MNT",
    "MOP",
    "MRO",
    "MUR",
    "MVR",
    "MWK",
    "MXN",
    "MYR",
    "MZN",
    "NAD",
    "NGN",
    "NIO",
    "NOK",
    "NPR",
    "NZD",
    "PAB",
    "PEN",
    "PGK",
    "PHP",
    "PKR",
    "PLN",
    "PYG",
    "QAR",
    "RON",
    "RSD",
    "RUB",
    "RWF",
    "SAR",
    "SBD",
    "SCR",
    "SEK",
    "SGD",
    "SHP",
    "SLL",
    "SOS",
    "SRD",
    "STD",
    "SZL",
    "THB",
    "TJS",
    "TOP",
    "TRY",
    "TTD",
    "TWD",
    "TZS",
    "UAH",
    "UGX",
    "UYU",
    "UZS",
    "VND",
    "VUV",
    "WST",
    "XAF",
    "XCD",
    "XOF",
    "XPF",
    "YER",
    "ZAR",
    "ZMW"
  ).flatMap(anyCurrencyOption)

  private def anyCurrencyOption(code: String) = Try(Currency.getInstance(code)).toOption
