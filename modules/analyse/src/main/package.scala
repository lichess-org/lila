package lila

package object analyse extends PackageObject with WithPlay {

  type InfoAdvices = List[(Info, Option[Advice])]

  type PgnMove = String
}
