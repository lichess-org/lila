package lila.game

// times are expressed in seconds
case class CorrespondenceClock(
    increment: Int,
    whiteTime: Float,
    blackTime: Float) {

  def emerg = 60 * 10
}
