package lila.plan

import java.util.Currency

import lila.memo.SettingStore

final class StripePaymentMethods(
    paymentMethods: SettingStore[lila.common.Strings]
) {

  val card = Set("card")

  def apply(mode: String, currency: Currency): Set[String] = mode match {
    case "setup" => card
    case "payment" | "subscription" =>
      all() filter {
        case "alipay" => Set(EUR, CNY)(currency)
        case _        => true
      }
    case _ => card
  }

  private val EUR = Currency getInstance "EUR"
  private val CNY = Currency getInstance "CNY"

  private def all(): Set[String] = card ++ paymentMethods.get().value
}
