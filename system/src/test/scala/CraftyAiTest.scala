package lila.system

import ai._

class CraftyAiTest extends AiTest {
  sequential

  def ai = new CraftyAi
  def name = "crafty"
  def nbMoves = 5
}
