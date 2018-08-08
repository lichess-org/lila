package lidraughts.common

// import scalaz.NonEmptyList
import scalaz._

case class Captcha(
    gameId: String,
    fen: String,
    white: Boolean,
    solutions: Captcha.Solutions,
    moves: Map[String, String]
) {

  def valid(solution: String) = solutions.toList contains solution
}

object Captcha {

  type Solutions = NonEmptyList[String]

  val default = Captcha(
    gameId = "00000000",
    fen = "W:W27,40,45:B12,13,14,19,24,26,34",
    white = true,
    solutions = NonEmptyList("40 29"),
    moves = Map("40" -> "29")
  )
}
