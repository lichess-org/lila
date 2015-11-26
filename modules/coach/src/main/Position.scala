package lila.coach

sealed trait Position {

  def tellNumber: String
}

object Position {

  case object Game extends Position {

    val tellNumber = "Number of games"
  }

  case object Move extends Position {

    val tellNumber = "Number of moves"
  }
}
