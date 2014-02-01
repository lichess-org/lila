package lila.game

import ornicar.scalalib.Random

object IdGenerator {

  def game = Random nextStringUppercase Game.gameIdSize

  def token = Random nextStringUppercase Game.tokenSize

  def player = Random nextStringUppercase Game.playerIdSize
}
