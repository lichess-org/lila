package lila

import lila.db.JsTube

package object security extends PackageObject with WithPlay {

  object tube {

    private[security] implicit lazy val storeTube =
      JsTube.json inColl Env.current.storeColl

    private[security] implicit lazy val firewallTube =
      JsTube.json inColl Env.current.firewallColl
  }
}
