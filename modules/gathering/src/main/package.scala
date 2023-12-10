package lila.gathering

export lila.Lila.{ *, given }

private val prizeRegex =
  """(?i)(prize|\$|€|£|¥|₽|元|₹|₱|₿|rupee|rupiah|ringgit|(\b|\d)usd|dollar|paypal|cash|award|\bfees?\b|\beuros?\b|price|(\b|\d)btc|bitcoin)""".r.unanchored

def looksLikePrize(txt: String) = prizeRegex matches txt
