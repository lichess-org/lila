package lila.common

import scalaz.{ NonEmptyList, NonEmptyLists, Zero, Zeros }

case class Captcha(
    gameId: String,
    fen: String,
    white: Boolean,
    solutions: Captcha.Solutions) {

  def valid(solution: String) = solutions.list contains solution
}

object Captcha extends NonEmptyLists {

  type Solutions = NonEmptyList[String]

  implicit val captchaZero = new Zero[Captcha] {
    val zero = Captcha(
      gameId = "00000000",
      fen = "1k3b1r/r5pp/pNQppq2/2p5/4P3/P3B3/1P3PPP/n4RK1",
      white = true,
      solutions = nel("c6 c8", Nil))
  }
}
