package lila
package ai.stockfish

final class Config {

  type Instructions = List[String]

  def init: Instructions = List(
    setoption("Hash", 1024),
    setoption("Threads", 8),
    setoption("Ponder", false)
  )

  def game(level: Int): Instructions = List(
    setoption("Skill Level", skill(level))
  )

  def moveTime(level: Int): Int = 
    (level >= 1 && level <= 8).fold(1000 / (9 - level), 100)

  private def skill(level: Int) = 
    (level >= 1 && level <= 8).fold(math.round((level -1) * (20 / 7f)), 0)

  private def setoption(name: String, value: Any) = 
    "setoption name %s value %s".format(name, value)
}
