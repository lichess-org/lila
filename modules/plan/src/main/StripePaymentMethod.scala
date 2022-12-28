package lila.plan

import java.util.Currency

import lila.memo.SettingStore

final class StripePaymentMethods(paymentMethods: SettingStore[lila.common.Strings]):

  val card = Set("card")

  def apply(mode: StripeMode, currency: Currency): Set[String] = mode match
    case StripeMode.setup => card
    case StripeMode.payment | StripeMode.subscription =>
      all() filter {
        case "alipay" => Set(EUR, CNY)(currency)
        case _        => true
      }

  private val EUR = Currency getInstance "EUR"
  private val CNY = Currency getInstance "CNY"

  private def all(): Set[String] = card ++ paymentMethods.get().value
