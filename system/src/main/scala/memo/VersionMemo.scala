package lila.system
package memo

import lila.chess.Color
import scalaz.Memo

final class VersionMemo {

  private val memo: String ⇒ Int = Builder.cache(1800)(compute)

  def get(gameId: String, color: Color): Int =
    memo(toKey(gameId, color))

  private def toKey(gameId: String, color: Color): String =
    gameId + ":" + color.name + ":v"

  private def fromKey(key: String): Option[(String, Color)] =
    key.split(':').toList match {
      case gameId :: cName :: "v" :: Nil ⇒ Color(cName) map { (gameId, _) }
      case _                             ⇒ None
    }

  private def compute(key: String): Int = fromKey(key) map {
    case (gameId, color) ⇒ compute(gameId, color)
  } getOrElse 0

  private def compute(gameId: String, color: Color): Int = 33
}
