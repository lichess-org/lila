package lila.insight

enum InsightPosition(val tellNumber: String, val short: String):

  def name = toString.toLowerCase

  case Game extends InsightPosition("Number of games", "games")
  case Move extends InsightPosition("Number of moves", "moves")
