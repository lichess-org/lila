package lila.common

import cats.data.NonEmptyList
import chess.format.BoardFen

case class Captcha(
    gameId: GameId,
    fen: BoardFen,
    color: chess.Color,
    solutions: Captcha.Solutions,
    moves: Map[String, String]
):

  def valid(solution: String) = solutions.toList contains solution

object Captcha:

  type Solutions = NonEmptyList[String]

  val default = Captcha(
    gameId = GameId("00000000"),
    fen = BoardFen("1k3b1r/r5pp/pNQppq2/2p5/4P3/P3B3/1P3PPP/n4RK1"),
    color = chess.White,
    solutions = NonEmptyList.one("c6 c8"),
    moves = Map("c6" -> "c8")
  )

  val failMessage = "captcha.fail"

  import scala.reflect.Selectable.reflectiveSelectable
  def isFailed(form: Form.FormLike) =
    form.errors.exists { _.messages has failMessage }
