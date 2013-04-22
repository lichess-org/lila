package lila

package object wiki extends PackageObject with WithPlay {

  object tube {

    private[wiki] implicit lazy val pageTube = Page.tube inColl Env.current.pageColl
  }
}
