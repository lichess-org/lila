package lila.analyse

case class Score(centipawns: Int) extends AnyVal {
  def pawns: Float = centipawns / 100f
  def showPawns: String = "%.2f" format pawns

  def ceiled =
    if (centipawns > Score.CEILING) copy(Score.CEILING)
    else if (centipawns < -Score.CEILING) copy(-Score.CEILING)
    else this

  def invert = copy(centipawns = -centipawns)
}

object Score {

  def CEILING = 1000

  def apply(str: String): Option[Score] = parseIntOption(str) map Score.apply
}
