package lila
package game

import ornicar.scalalib.Random

object IdGenerator {

  def game = Random nextString DbGame.gameIdSize

  def player = Random nextString DbGame.playerIdSize
}
