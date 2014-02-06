package lila

package object puzzle extends PackageObject with WithPlay {

  type PuzzleId = Int
  type AttemptId = Int
  type Lines = List[Line]
}
