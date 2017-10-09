package lila

package object puzzle extends PackageObject {

  type PuzzleId = Int
  type RoundId = Int
  type Lines = List[Line]

  private[puzzle] def logger = lila.log("puzzle")

  case class Result(win: Boolean) extends AnyVal {

    def loss = !win
  }
}
