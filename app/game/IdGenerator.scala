package lila.app
package game

import ornicar.scalalib.Random

object IdGenerator {

  def game = Random nextString DbGame.gameIdSize

  def token = Random nextString DbGame.tokenSize

  def player = Random nextString DbGame.playerIdSize
}
