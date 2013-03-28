package lila

import lila.db.InColl

package object security extends PackageObject with WithPlay {

  private[security] sealed trait Visit

  private[security] lazy val storeInColl = 
    InColl[Visit](Env.current.storeColl)
}
