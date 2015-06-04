package lila.user

import org.joda.time.DateTime

import lila.db.BSON.BSONJodaDateTimeHandler
import reactivemongo.bson._

final class TrophyApi(coll: lila.db.Types.Coll) {

  private implicit val trophyKindBSONHandler = new BSONHandler[BSONString, Trophy.Kind] {
    def read(bsonString: BSONString): Trophy.Kind =
      Trophy.Kind byKey bsonString.value err s"No such trophy kind: ${bsonString.value}"
    def write(x: Trophy.Kind) = BSONString(x.key)
  }
  private implicit val trophyBSONHandler = Macros.handler[Trophy]

  def insert(user: User, kind: Trophy.Kind): Fu[Trophy] = {
    val trophy = Trophy.make(user, kind)
    coll insert trophy inject trophy
  }

  def findByUser(user: User, max: Int = 12): Fu[List[Trophy]] =
    coll.find(BSONDocument("user" -> user.id)).cursor[Trophy].collect[List](max)
}
