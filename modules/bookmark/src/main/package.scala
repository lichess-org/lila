package lila

import lila.db.Tube

package object bookmark extends PackageObject with WithPlay {

  object tube {

    private[bookmark] implicit lazy val bookmarkTube =
      Tube.json inColl Env.current.bookmarkColl
  }
}
