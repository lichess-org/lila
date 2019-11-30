package lila.user

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.memo._
import org.joda.time.DateTime
import reactivemongo.api.bson._
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

  private implicit val trophyKindStringBSONHandler = lila.db.BSON.quickHandler[TrophyKind](
    { case BSONString(str) => kindCache sync str },
    x => BSONString(x._id)
  )

  private implicit val trophyBSONHandler = Macros.handler[Trophy]

  def findByUser(user: User, max: Int = 50): Fu[List[Trophy]] =
    coll.ext.find($doc("user" -> user.id)).list[Trophy](max).map(_.filter(_.kind != TrophyKind.Unknown))

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
    coll.insert.one($doc(
      "_id" -> ornicar.scalalib.Random.nextString(8),
      "user" -> userId,
      "kind" -> kindKey,
      "date" -> DateTime.now
    )) void
}
