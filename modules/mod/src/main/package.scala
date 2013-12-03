package lila

import lila.db.JsTube

package object mod extends PackageObject with WithPlay {

  object tube {

    private[mod] implicit lazy val modlogTube =
      Modlog.tube inColl Env.current.modlogColl
  }
}
