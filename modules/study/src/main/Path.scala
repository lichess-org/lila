package lila.study

import scalaz.NonEmptyList

case class Path(crumbs: NonEmptyList[Crumb])

case class Crumb(ply: Int, variation: Option[Int])

object Crumb {

  val init = Crumb(0, None)
}

object Path {

  val init = Path(NonEmptyList(Crumb.init))
}
