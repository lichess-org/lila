package lila

import lila.db.JsTube

package object security extends PackageObject with WithPlay {

  object tube {

    private[security] def storeColl = Env.current.storeColl

    private[security] implicit lazy val firewallTube =
      JsTube.json inColl Env.current.firewallColl
  }

  private[security] def logger = lila.log("security")
}
