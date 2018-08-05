package lila.qa

import reactivemongo.bson._

import lila.db.dsl._

final class Search(collection: Coll) {

  import QaApi._

  def apply(q: String): Fu[List[Question]] =
    collection.find(BSONDocument("$text" -> BSONDocument("$search" -> q)))
      .cursor[Question]().list
}
