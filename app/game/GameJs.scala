package lila
package game

final class GameJs(path: String, useCache: Boolean) {

  def unsigned: String = useCache.fold(cached, readFromSource)

  val placeholder = "--tkph--"

  def sign(token: String) = unsigned.replace(placeholder, token)

  private lazy val cached: String = readFromSource

  private def readFromSource = {
    val source = scala.io.Source fromFile path
    source.mkString ~ { _ ⇒ source.close }
  }
}
