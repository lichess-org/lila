package lila

import ai._

class CraftyAiTest extends AiTest {
  sequential

  val ai = SystemEnv().craftyAi
  def name = "crafty"
  def nbMoves = 5
}
