package lila

import lila.db.JsTube

package object bookmark extends PackageObject with WithPlay {

  object tube {

    private[bookmark] implicit lazy val bookmarkTube =
      JsTube.json inColl Env.current.bookmarkColl
  }
}
