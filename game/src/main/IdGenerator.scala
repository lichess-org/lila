package lila.game

import ornicar.scalalib.Random

object IdGenerator {

  def game = Random nextString Game.gameIdSize

  def token = Random nextString Game.tokenSize

  def player = Random nextString Game.playerIdSize
}
