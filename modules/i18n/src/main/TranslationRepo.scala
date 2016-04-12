package lila.i18n

import play.api.libs.json.Json

import lila.db.dsl._
import lila.db.BSON.BSONJodaDateTimeHandler

private[i18n] final class TranslationRepo(coll: Coll) {

  private implicit val TranslationBSONHandler = reactivemongo.bson.Macros.handler[Translation]

  def nextId: Fu[Int] = coll.primitiveOne[Int](
    selector = $empty,
    sort = $sort desc "_id",
    "_id") map (opt => ~opt + 1)

  def findFrom(id: Int): Fu[List[Translation]] =
    coll.find($doc("_id" $gte id)).sort($sort asc "_id").cursor[Translation]().gather[List]()

  def insert(t: Translation) = coll insert t
}
