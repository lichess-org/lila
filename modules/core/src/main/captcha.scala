package lila.core
package captcha

import chess.format.BoardFen

type Solutions = NonEmptyList[String]

case class Captcha(
    gameId: GameId,
    fen: BoardFen,
    color: chess.Color,
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
