package lila
package game

private[game] final class GameJs(path: String) {

  def unsigned: String = {
    val source = scala.io.Source fromFile path
    source.mkString ~ { _ ⇒ source.close }
  }

  val placeholder = "--tkph--"

  def sign(token: String) = unsigned.replace(placeholder, token)
}
