package lila.core
package captcha

import chess.format.BoardFen
import play.api.data.Form

type Solutions = NonEmptyList[String]

case class Captcha(
    gameId: GameId,
    fen: BoardFen,
    color: chess.Color,
    solutions: Solutions,
    moves: Map[String, String]
)

val failMessage = "captcha.fail"

import scala.reflect.Selectable.reflectiveSelectable
def isFailedFixMe(form: lila.common.Form.FormLike) =
  form.errors.exists { _.messages.has(failMessage) }

trait WithCaptcha:
  def gameId: GameId
  def move: String

trait CaptchaApi:
  def any: Captcha
  def get(id: GameId): Fu[Captcha]
  def validate(gameId: GameId, move: String): Fu[Boolean]
  def validateSync(data: WithCaptcha): Boolean
