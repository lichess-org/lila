package lila.opening

import scala.concurrent.duration._
import scala.util.Random

import reactivemongo.api.QueryOpts
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.Count

import lila.db.Types.Coll
import lila.user.User

private[opening] final class Selector(
    openingColl: Coll,
    api: OpeningApi) {

  val anonSkipMax = 10000

  def apply(me: Option[User]): Fu[Opening] = me match {
    case None =>
      openingColl.find(BSONDocument())
        .options(QueryOpts(skipN = Random nextInt anonSkipMax))
        .one[Opening] flatten "Can't find a opening for anon player!"
    case Some(user) => api.attempt playedIds user flatMap { ids =>
      openingColl.find(BSONDocument(
        Opening.BSONFields.id -> BSONDocument("$nin" -> ids)
      )).one[Opening] flatten s"Can't find a opening for user $user!"
    }
  }
}
