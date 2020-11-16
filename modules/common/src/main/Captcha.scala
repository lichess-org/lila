package lila.common

import scalaz.NonEmptyList

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
    fen = "9/k1b6/1PH6/9/9/9/9/9/9",
    white = true,
    solutions = NonEmptyList("c7 b8"),
    moves = Map("c7" -> "b8")
  )

  val failMessage = "captcha.fail"

  import scala.language.reflectiveCalls
  def isFailed(form: Form.FormLike) =
    form.errors.exists { _.messages has failMessage }
}
