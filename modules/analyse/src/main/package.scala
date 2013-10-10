package lila

package object analyse extends PackageObject with WithPlay {

  type InfoAdvices = List[(Info, Option[Advice])]

  type PgnMove = String

  object tube {

    private[analyse] implicit lazy val analysisTube =
      Analysis.tube inColl Env.current.analysisColl
  }
}
