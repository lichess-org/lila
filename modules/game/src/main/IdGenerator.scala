package lila.game

import ornicar.scalalib.Random

object IdGenerator {

  def uncheckedGame: Game.ID = Random nextString Game.gameIdSize

  def game: Fu[Game.ID] = {
    val id = uncheckedGame
    GameRepo.exists(id).flatMap {
      case true => game
      case false => fuccess(id)
    }
  }

  def player: Player.ID = Random secureString Game.playerIdSize
}
