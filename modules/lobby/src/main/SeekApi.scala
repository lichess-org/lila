package lila.lobby

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import reactivemongo.core.commands._

import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

final class SeekApi(
    coll: Coll,
    blocking: String => Fu[Set[String]],
  maxPerPage: Int) {

  def forAnon: Fu[List[Seek]] =
    coll.find(BSONDocument()).cursor[Seek].collect[List](maxPerPage)

  def forUser(user: User): Fu[List[Seek]] =
    blocking(user.id) flatMap { blocked =>
      forAnon map {
        _ filter { seek =>
          !seek.user.blocking.contains(user.id) &&
            !blocked.contains(seek.user.id)
        }
      }
    }

  def find(id: String): Fu[Option[Seek]] =
    coll.find(BSONDocument("_id" -> id)).one[Seek]

  def insert(seek: Seek) = coll.insert(seek).void

  def remove(seek: Seek) = coll.remove(BSONDocument("_id" -> seek.id)).void

  def removeBy(seekId: String, userId: String) =
    coll.remove(BSONDocument(
      "_id" -> seekId,
      "user.id" -> userId
    )).void
}
