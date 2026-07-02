package lila.gathering

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val prizeRegex =
  """(?i)(prize|\$|€|£|¥|₽|元|₹|₱|₿|rupee|rupiah|ringgit|(\b|\d)usd|dollar|paypal|cash|award|\bfees?\b|\beuros?\b|price|(\b|\d)btc\b|bitcoin)""".r.unanchored

def looksLikePrize(txt: String) =
  prizeRegex.findFirstIn(txt)
  prizeRegex.matches(txt)
