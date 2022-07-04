package lila.insight

sealed trait InsightPosition {

  def tellNumber: String

  def name = toString.toLowerCase
}

object InsightPosition {

  case object Game extends InsightPosition {

    val tellNumber = "Number of games"
  }

  case object Move extends InsightPosition {

    val tellNumber = "Number of moves"
  }
}
