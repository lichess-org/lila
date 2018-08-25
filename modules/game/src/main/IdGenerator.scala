package lila.game

import ornicar.scalalib.Random

object IdGenerator {

  def uncheckedGame = Random nextString Game.gameIdSize

  def player = Random secureString Game.playerIdSize
}
