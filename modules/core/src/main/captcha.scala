package lila.core
package captcha

import _root_.chess.Color
import _root_.chess.format.BoardFen

import lila.core.id.GameId

type Solutions = NonEmptyList[String]

case class Captcha(
    gameId: GameId,
    fen: BoardFen,
    color: Color,
    solutions: Solutions,
    moves: Map[String, String]
)

val failMessage = "captcha.fail"

trait WithCaptcha:
  def gameId: GameId
  def move: String

trait CaptchaApi:
  def any: Captcha
  def get(id: GameId): Fu[Captcha]
  def validate(gameId: GameId, move: String): Fu[Boolean]
  def validateSync(data: WithCaptcha): Boolean
