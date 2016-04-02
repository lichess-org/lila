package lila.user

import org.joda.time.DateTime

import lila.db.dsl._
import lila.db.BSON.BSONJodaDateTimeHandler
import reactivemongo.bson._

final class TrophyApi(coll: Coll) {

  private implicit val trophyKindBSONHandler = new BSONHandler[BSONString, Trophy.Kind] {
    def read(bsonString: BSONString): Trophy.Kind =
      Trophy.Kind byKey bsonString.value err s"No such trophy kind: ${bsonString.value}"
    def write(x: Trophy.Kind) = BSONString(x.key)
  }
  private implicit val trophyBSONHandler = Macros.handler[Trophy]

  def award(userId: String, kind: Trophy.Kind): Funit =
    coll insert Trophy.make(userId, kind) void

  def award(userId: String, kind: Trophy.Kind.type => Trophy.Kind): Funit =
    award(userId, kind(Trophy.Kind))

  def awardMarathonWinner(userId: String): Funit = award(userId, Trophy.Kind.MarathonWinner)

  def findByUser(user: User, max: Int = 12): Fu[List[Trophy]] =
    coll.find(BSONDocument("user" -> user.id)).cursor[Trophy]().gather[List](max)
}
