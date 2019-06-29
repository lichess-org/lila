package lila.user

import lila.db.dsl._
import lila.db.BSON.BSONJodaDateTimeHandler
import reactivemongo.bson._

final class TrophyApi(coll: Coll, kindColl: Coll) {
  private implicit val trophyBSONHandler = Macros.handler[SimplifiedTrophy]
  private implicit val trophyKindBSONHandler = Macros.handler[TrophyKind]

  def findByUser(user: User, max: Int = 50): Fu[List[Trophy]] =
    coll.find($doc("user" -> user.id)).list[SimplifiedTrophy](max) flatMap { l =>
      unsimplifyList(l)
    }

  def roleBasedTrophies(user: User, isPublicMod: Boolean, isDev: Boolean, isVerified: Boolean): Fu[List[Trophy]] = List(
    isPublicMod option unsimplify(SimplifiedTrophy(
      _id = "",
      user = user.id,
      kind = TrophyKind.moderator,
      date = org.joda.time.DateTime.now
    )),
    isDev option unsimplify(SimplifiedTrophy(
      _id = "",
      user = user.id,
      kind = TrophyKind.developer,
      date = org.joda.time.DateTime.now
    )),
    isVerified option unsimplify(SimplifiedTrophy(
      _id = "",
      user = user.id,
      kind = TrophyKind.verified,
      date = org.joda.time.DateTime.now
    ))
  ).flatten.sequenceFu.map(_.flatten)

  def unsimplifyList(simplified: List[SimplifiedTrophy]): Fu[List[Trophy]] =
    simplified.map({ t =>
      unsimplify(t)
    }).sequenceFu.map(_.flatten)

  def unsimplify(simplified: SimplifiedTrophy): Fu[Option[Trophy]] =
    kindColl.byId[TrophyKind](simplified.kind) map2 { (kind: TrophyKind) =>
      Trophy(
        _id = simplified._id,
        user = simplified.user,
        kind = kind,
        date = simplified.date
      )
    }

  def award(userId: String, kindKey: String): Funit =
    coll insert SimplifiedTrophy.make(userId, kindKey) void
}
