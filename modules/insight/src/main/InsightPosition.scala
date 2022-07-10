package lila.insight

sealed trait InsightPosition {

  def tellNumber: String
  def short: String

  def name = toString.toLowerCase
}

object InsightPosition {

  case object Game extends InsightPosition {

    val tellNumber = "Number of games"

    val short = "games"
  }

  case object Move extends InsightPosition {

    val tellNumber = "Number of moves"

    val short = "moves"
  }
}
