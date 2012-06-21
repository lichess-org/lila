package lila
package game

import ornicar.scalalib.OrnicarRandom

object IdGenerator {

  def game = OrnicarRandom nextString DbGame.gameIdSize

  def player = OrnicarRandom nextString DbGame.playerIdSize
}
