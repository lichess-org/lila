package lila.user

import lila.db.dsl._
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.memo._
import reactivemongo.bson._
import org.joda.time.DateTime
import scala.concurrent.duration._

final class TrophyApi(
    coll: Coll, kindColl: Coll
)(implicit system: akka.actor.ActorSystem) {

  private val trophyKindObjectBSONHandler = Macros.handler[TrophyKind]

  val kindCache = new Syncache[String, TrophyKind](
    name = "trophy.kind",
    compute = id => kindColl.byId(id)(trophyKindObjectBSONHandler) map { k => k.getOrElse(TrophyKind.Unknown) },
    default = _ => TrophyKind.Unknown,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(1 hour),
    logger = logger
  )

  private implicit val trophyKindStringBSONHandler = new BSONHandler[BSONString, TrophyKind] {
    def read(bsonString: BSONString): TrophyKind =
      kindCache sync bsonString.value
    def write(x: TrophyKind) = BSONString(x._id)
  }

  private implicit val trophyBSONHandler = Macros.handler[Trophy]

  def findByUser(user: User, max: Int = 50): Fu[List[Trophy]] =
    coll.find($doc("user" -> user.id)).list[Trophy](max).map(_.filter(_.kind != TrophyKind.Unknown))

  def roleBasedTrophies(user: User, isPublicMod: Boolean, isDev: Boolean, isVerified: Boolean): List[Trophy] = List(
    isPublicMod option Trophy(
      _id = "",
      user = user.id,
      kind = kindCache sync TrophyKind.moderator,
      date = org.joda.time.DateTime.now
    ),
    isDev option Trophy(
      _id = "",
      user = user.id,
      kind = kindCache sync TrophyKind.developer,
      date = org.joda.time.DateTime.now
    ),
    isVerified option Trophy(
      _id = "",
      user = user.id,
      kind = kindCache sync TrophyKind.verified,
      date = org.joda.time.DateTime.now
    )
  ).flatten

  def award(userId: String, kindKey: String): Funit =
    coll.insert(BSONDocument(
      "_id" -> ornicar.scalalib.Random.nextString(8),
      "user" -> userId,
      "kind" -> kindKey,
      "date" -> DateTime.now
    )) void
}
