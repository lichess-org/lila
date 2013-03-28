package lila

import lila.db.{ Tube, InColl }

package object game extends PackageObject with WithPlay {

  private[game] lazy val pgnInColl = InColl(Env.current.pgnColl)

  private[game] lazy val gameTube = Game.tube inColl Env.current.gameColl
}
