package lila.streamer

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.db.Photographer
import lila.user.{ User, UserRepo }

final class StreamerApi(coll: Coll, photographer: Photographer) {

  import BsonHandlers._
}
