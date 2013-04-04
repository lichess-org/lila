package lila

import lila.db.{ Tube, InColl }

package object game extends PackageObject with WithPlay {

  object tube {

    private[game] implicit lazy val pgnTube = 
      Tube.json inColl Env.current.pgnColl

    // expose game tube
    implicit lazy val gameTube = Game.tube inColl Env.current.gameColl
  }
}
