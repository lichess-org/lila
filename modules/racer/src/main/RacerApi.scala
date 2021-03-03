package lila.racer

import lila.db.dsl._
import scala.concurrent.ExecutionContext
import lila.user.User
import lila.socket.Socket

final class RacerApi(coll: Coll)(implicit ec: ExecutionContext) {

  import RacerBsonHandlers._
  import RacerRace.{ Id, Owner }

  def create(sri: Socket.Sri, user: Option[User]): Fu[RacerRace] = {
    val race = RacerRace.make {
      user match {
        case Some(u) => Owner.User(u.id)
        case None    => Owner.Anon(sri)
      }
    }
    coll.insert.one(race) inject race
  }

  def get(id: Id): Fu[Option[RacerRace]] =
    coll.byId[RacerRace](id.value)
}
