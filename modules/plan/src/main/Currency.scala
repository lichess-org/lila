package lila.plan

import io.methvin.play.autoconfig.AutoConfig
import java.util.{ Currency, Locale }
import play.api.libs.json._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.Try

import lila.common.config
import play.api.i18n.Lang

case class CurrencyWithRate(currency: Currency, rate: Double)

final class CurrencyApi(
    ws: StandaloneWSClient,
    mongoCache: lila.memo.MongoCache.Api,
    config: CurrencyApi.Config
)(implicit ec: ExecutionContext) {

  private val baseUrl = "https://openexchangerates.org/api"

  private val ratesCache = mongoCache.unit[Map[String, Double]](
    "currency:rates",
    60 minutes // i.e. 744/month, under the 1000/month limit of free OER plan
  ) { loader =>
    _.refreshAfterWrite(61 minutes)
      .buildAsyncFuture {
        loader { _ =>
          ws.url(s"$baseUrl/latest.json")
            .withQueryStringParameters("app_id" -> config.appId.value)
            .get()
            .dmap { res =>
              (res.body[JsValue] \ "rates").validate[Map[String, Double]].asOpt.fold(Map("USD" -> 1d)) {
                _.filterValues(0 <)
              }
            }
        }
      }
  }

  def convert(money: Money, to: Locale): Fu[Option[Money]] =
    ratesCache.get {} map { rates =>
      for {
        currency <- Try(Currency getInstance to).toOption.pp(to.toString)
        fromRate <- rates get money.currency.getCurrencyCode
        toRate   <- rates get currency.getCurrencyCode
      } yield Money(money.amount / fromRate * toRate, to)
    }

  val US  = Locale.US
  val USD = Currency getInstance US

  def localeByCountryCodeOrLang(countryCode: Option[String], lang: Lang): Locale =
    countryCode
      .flatMap { code => scala.util.Try(new java.util.Locale("", code)).toOption }
      .filter(hasCurrency)
      .orElse(lang.locale.some)
      .filter(hasCurrency)
      .getOrElse(US)

  private def hasCurrency(locale: Locale) = Try(Currency getInstance locale).isSuccess
}

private object CurrencyApi {

  case class Config(appId: config.Secret)
  implicit val currencyConfigLoader = AutoConfig.loader[Config]
}
