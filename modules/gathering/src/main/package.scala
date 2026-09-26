package lila.gathering

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val prizeRegex =
  """(?i)(prize|\$|€|£|¥|₽|元|₹|₱|₿|rupee|rupiah|ringgit|(\b|\d)usd|dollar|paypal|cash|award|\bfees?\b|\beuros?\b|price|(\b|\d)btc\b|bitcoin)""".r.unanchored

def looksLikePrize(txt: String) = prizeRegex.matches(txt)

opaque type Payouts = String
object Payouts extends OpaqueString[Payouts]:
  extension (p: Payouts)
    def nbWinners: Int = p.count(_ == '/') + 1
    def nonEmptyOption: Option[Payouts] = Option.when(p.value.nonEmpty)(p)
