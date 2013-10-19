package lila

import lila.db.Tube

package object pref extends PackageObject with WithPlay {

  object tube {

    private[pref] implicit lazy val prefTube =
      Pref.tube inColl Env.current.prefColl
  }
}
