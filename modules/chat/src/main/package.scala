package lila

package object chat extends PackageObject with WithPlay {

  object tube {

    implicit lazy val lineTube = Line.tube inColl Env.current.lineColl
  }
}
