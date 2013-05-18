package lila.round

import lila.game.{ Query, Game, GameRepo }
import lila.game.tube.gameTube
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round.Outoftime
import lila.db.api._
import lila.common.PimpedJson._

import akka.actor.ActorRef
import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

private[round] final class Titivate(
    roundMap: ActorRef,
    meddler: Meddler) {

  def finishByClock: Funit =
    $enumerate.bulk[Option[Game]]($query(Query.candidatesToAutofinish), 50) { games ⇒
      fuloginfo("[titivate] Finish %d games by clock" format games.flatten.size) >>-
        (games.flatten foreach { game ⇒ roundMap ! Tell(game.id, Outoftime) })
    }

  def finishAbandoned: Funit =
    $enumerate.bulk[Option[Game]]($query(Query.abandoned), 50) { games ⇒
      fuloginfo("[titivate] Finish %d abandoned games" format games.flatten.size) >>-
        (games.flatten foreach meddler.finishAbandoned)
    }
}
