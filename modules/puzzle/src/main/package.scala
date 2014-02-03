package lila

package object puzzle extends PackageObject with WithPlay {

  private[puzzle] type PuzzleId = String

  private[puzzle] type Lines = List[Line]
}
