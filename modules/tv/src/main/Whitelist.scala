package lila.tv

import lila.db.Types._
import reactivemongo.bson._

private final class Whitelist(coll: Coll) {

  def apply: Fu[Set[String]] =
    coll.find(BSONDocument("enabled" -> true)).cursor[BSONDocument].collect[List]().map2 { (obj: BSONDocument) =>
      obj.getAs[String]("_id")
    }.map(_.flatten.toSet)

  def withChat(id: String): Fu[Boolean] =
    coll.find(BSONDocument("_id" -> id, "chat" -> true)).one[BSONDocument].map(_.isDefined)
}
