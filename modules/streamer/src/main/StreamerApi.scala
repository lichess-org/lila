package lila.streamer

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.db.Photographer
import lila.user.{ User, UserRepo }

final class StreamerApi(coll: Coll, photographer: Photographer) {

  import BsonHandlers._

  def save(s: Streamer): Funit =
    coll.update($id(s.id), s, upsert = true).void
}
