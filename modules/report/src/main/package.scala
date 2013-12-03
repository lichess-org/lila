package lila

import lila.db.JsTube

package object report extends PackageObject with WithPlay {

  object tube {

    private[report] implicit lazy val reportTube =
      Report.tube inColl Env.current.reportColl
  }
}
