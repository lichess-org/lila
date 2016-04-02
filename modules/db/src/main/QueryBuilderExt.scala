package lila.db

import reactivemongo.api._
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.bson._

trait QueryBuilderExt { self: dsl =>

  final implicit class ExtendQueryBuilder[A](val b: dsl.QueryBuilder) {

    def skip(nb: Int) = b.options(b.options skip nb)

    def batch(nb: Int) = b.options(b.options batchSize nb)

    def toList[A: BSONDocumentReader](limit: Option[Int]): Fu[List[A]] =
      limit.fold(b.cursor[A]().collect[List]()) { l =>
        b.cursor[A]().collect[List](l)
      }
  }
}
