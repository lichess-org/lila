package lila
package format

import model.Game

trait Format {

  def <<(source: String): Game

  def >>(game: Game): String
}
