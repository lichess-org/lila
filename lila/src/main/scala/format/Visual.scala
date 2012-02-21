package lila
package format

import model._

object Visual extends Format {

  def <<(source: String): Game = Game("fromsource")

  def >>(game: Game): String = ""
}
