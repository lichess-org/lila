package lila.qa

import reactivemongo.bson._

import lila.db.dsl._

final class Search(collection: Coll) {

  private implicit val commentBSONHandler = Macros.handler[Comment]
  private implicit val voteBSONHandler = Macros.handler[Vote]
  private[qa] implicit val questionBSONHandler = Macros.handler[Question]

  def apply(q: String): Fu[List[Question]] =
    collection.find(BSONDocument(
      "$text" -> BSONDocument("$search" -> q)
    )).cursor[Question]().gather[List]()
}
