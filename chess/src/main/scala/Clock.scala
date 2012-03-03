package lila.chess

case class Clock(
    color: Color,
    increment: Int,
    limit: Int,
    times: Map[Color, Float]) {
}
