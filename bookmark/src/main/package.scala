package lila

import lila.db.InColl

package object bookmark extends PackageObject with WithPlay {

  private[bookmark] val bookmarkInColl = InColl(Env.current.bookmarkColl)
}
