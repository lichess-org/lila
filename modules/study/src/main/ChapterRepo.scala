package lila.study

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import scala.concurrent.duration._

import lila.db.dsl._

private final class ChapterRepo(coll: Coll) {

  import BSONHandlers._

  def byId(id: Chapter.ID): Fu[Option[Chapter]] = coll.byId[Chapter](id)

  def exists(id: Chapter.ID) = coll.exists($id(id))

  def insert(s: Chapter): Funit = coll.insert(s).void

  def update(c: Chapter): Funit = coll.update($id(c.id), c).void
}
