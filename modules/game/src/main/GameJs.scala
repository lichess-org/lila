package lila.game

private[game] final class GameJs(path: String, useCache: Boolean) {

  def unsigned: String = useCache.fold(cached, readFromSource)

  val placeholder = "--ranph--"

  def sign(token: String) = unsigned.replace(placeholder, token)

  private lazy val cached: String = readFromSource

  private def readFromSource = {
    val source = scala.io.Source fromFile path
    source.mkString ~ { _ => source.close }
  }
}
