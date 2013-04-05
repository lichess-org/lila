package lila

import lila.db.Tube

package object security extends PackageObject with WithPlay {

  object tube {

    private[security] implicit lazy val storeTube =
      Tube.json inColl Env.current.storeColl

    private[security] implicit lazy val firewallTube =
      Tube.json inColl Env.current.firewallColl
  }
}
