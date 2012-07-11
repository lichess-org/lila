package lila
package ai.stockfish

trait Config {

  protected def setoption(name: String, value: Any) = 
    "setoption name %s value %s".format(name, value)
}

object Config {

  val levels = 1 to 8

  val levelBox = intBox(1 to 8) _
}
