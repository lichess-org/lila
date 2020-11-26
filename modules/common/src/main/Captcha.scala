package lila.common

import cats.data.NonEmptyList

case class Captcha(
    gameId: String,
    fenBoard: String,
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
    fenBoard = "1k3b1r/r5pp/pNQppq2/2p5/4P3/P3B3/1P3PPP/n4RK1",
    white = true,
    solutions = NonEmptyList.one("c6 c8"),
    moves = Map("c6" -> "c8")
  )

  val failMessage = "captcha.fail"

  import scala.language.reflectiveCalls
  def isFailed(form: Form.FormLike) =
    form.errors.exists { _.messages has failMessage }
}
