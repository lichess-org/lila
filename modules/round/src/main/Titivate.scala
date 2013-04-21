package lila.round

import lila.game.GameRepo
import lila.game.tube.gameTube
import lila.db.api._
import lila.common.PimpedJson._

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

private[round] final class Titivate(finisher: Finisher) {

  // TODO
  def finishByClock: Funit = GameRepo.candidatesToAutofinish flatMap { games ⇒
    fuloginfo("[titivate] Finish %d games by clock" format games.size) >>
      funit //(finisher outoftimes games).sequence
  }

  // val finishAbandoned: Funit = for {
  //   games ← gameRepo abandoned 300
  //   _ ← putStrLn("[titivate] Finish %d abandoned games" format games.size)
  //   _ ← (games map meddler.finishAbandoned).sequence
  // } yield ()
}
