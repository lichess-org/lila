package lila.insight

enum InsightPosition(val tellNumber: String, val short: String):

  case Game extends InsightPosition("Number of games", "games")
  case Move extends InsightPosition("Number of moves", "moves")

  def name = toString.toLowerCase
