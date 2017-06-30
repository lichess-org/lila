package lila

package object game extends PackageObject with WithPlay {

  type PgnMoves = Vector[String]

  private[game] def logger = lila.log("game")
}
