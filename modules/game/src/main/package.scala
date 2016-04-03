package lila

package object game extends PackageObject with WithPlay {

  type PgnMoves = List[String]

  private[game] def logger = lila.log("game")
}
