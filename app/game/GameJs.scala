package lila
package game

final class GameJs(path: String) { 

  lazy val unsigned: String = {
    val source = scala.io.Source fromFile path
    source.mkString ~ { _ => source.close }
  }

  val placeholder = "--tkph--"

  def sign(token: String) = unsigned.replace(placeholder, token)
}
