package lila

import lila.db.{ Tube, InColl }

package object game extends PackageObject with WithPlay {

  object tube {

    implicit lazy val pgnTube = Tube.json inColl Env.current.pgnColl

    implicit lazy val gameTube = Game.tube inColl Env.current.gameColl
  }
}
