package lila

import lila.db.{ JsTube, InColl }

package object game extends PackageObject with WithPlay {

  type PgnMoves = List[String]

  object tube {

    implicit lazy val gameTube = Game.tube inColl Env.current.gameColl
  }

  private[game] def logger = lila.log("game")
}
