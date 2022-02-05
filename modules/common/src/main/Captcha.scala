package lila.common

import cats.data.NonEmptyList

case class Captcha(
    gameId: String,
    sfenBoard: String,
    sente: Boolean,
    solutions: Captcha.Solutions,
    moves: Map[String, String]
) {

  def valid(solution: String) = solutions.toList contains solution
}

object Captcha {

  type Solutions = NonEmptyList[String]

  val default = Captcha(
    gameId = "00000000",
    sfenBoard = "9/k1b6/1P+B6/9/9/9/9/9/9",
    sente = true,
    solutions = NonEmptyList.one("7c 8b"),
    moves = Map("7c" -> "8b 6d", "8c" -> "8b")
  )

  val failMessage = "captcha.fail"

  import scala.language.reflectiveCalls
  def isFailed(form: Form.FormLike) =
    form.errors.exists { _.messages has failMessage }
}
