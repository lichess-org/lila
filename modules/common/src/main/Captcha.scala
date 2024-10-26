package lila.common

import cats.data.NonEmptyList

case class Captcha(
    gameId: String,
    sfenBoard: String,
    sente: Boolean,
    solutions: Captcha.Solutions
) {

  def valid(solution: String) = solutions.toList contains solution

  def hint = solutions.head.take(2)
}

object Captcha {

  type Solutions = NonEmptyList[String]

  val default = Captcha(
    gameId = "00000000",
    sfenBoard = "4k/4p/3R1/GG3/KG1R1",
    sente = true,
    solutions = NonEmptyList.one("2c2a")
  )

  val failMessage = "captcha.fail"

  import scala.language.reflectiveCalls
  def isFailed(form: Form.FormLike) =
    form.errors.exists { _.messages has failMessage }
}
