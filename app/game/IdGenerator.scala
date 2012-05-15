package lila
package game

import ornicar.scalalib.OrnicarRandom

object IdGenerator {

  def game = OrnicarRandom nextAsciiString DbGame.gameIdSize

  def player = OrnicarRandom nextAsciiString DbGame.playerIdSize
}
