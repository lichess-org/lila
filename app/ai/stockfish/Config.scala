package lila
package ai.stockfish

trait Config {

  protected def setoption(name: String, value: Any) = 
    "setoption name %s value %s".format(name, value)
}
