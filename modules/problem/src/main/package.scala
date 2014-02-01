package lila

package object problem extends PackageObject with WithPlay {

  private[problem] type ProblemId = String

  private[problem] type Lines = List[Line]
}
