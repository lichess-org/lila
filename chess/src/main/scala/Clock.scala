package lila.chess

case class Clock(
    color: Color,
    increment: Int,
    limit: Int,
    whiteTime: Float,
    blackTime: Float) {

  val times: Map[Color, Float] = Map(White -> whiteTime, Black -> blackTime)
}
