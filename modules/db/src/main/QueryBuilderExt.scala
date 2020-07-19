package lila.db

import scala.collection.Factory

import scala.concurrent.ExecutionContext

import reactivemongo.api._ /*,
  bson.BSONDocumentReader,
  bson.collection.BSONSerializationPack,
  collections.QueryBuilderFactory*/

trait QueryBuilderExt { self: dsl =>
  implicit final class ExtendCursor[A](cursor: Cursor.WithOps[A])(implicit ec: ExecutionContext) { // CursorProducer?

    def gather[M[_]](upTo: Int)(implicit factory: Factory[A, M[A]]): Fu[M[A]] =
      cursor.collect[M](upTo, Cursor.ContOnError[M[A]]())

    def list(): Fu[List[A]] =
      gather[List](Int.MaxValue)

    def list(limit: Int): Fu[List[A]] =
      gather[List](limit)

    def list(limit: Option[Int]): Fu[List[A]] =
      gather[List](limit | Int.MaxValue)

    def vector(limit: Int): Fu[Vector[A]] =
      gather[Vector](limit)

  }

  /*
  implicit final class ExtendQueryBuilder[B <: QueryBuilderFactory[BSONSerializationPack.type]#QueryBuilder](b: B)(implicit ec: ExecutionContext) {
    // like collect, but with stopOnError defaulting to false
    def gather[A, M[_]](upTo: Int, readPreference: ReadPreference = ReadPreference.primary)(implicit
        factory: Factory[A, M[A]],
        reader: BSONDocumentReader[A],
        cp: CursorProducer[A]
    ): Fu[M[A]] =
      b.cursor[A](readPreference = readPreference)(reader, cp)
        .collect[M](upTo, Cursor.ContOnError[M[A]]())

    def list[A: BSONDocumentReader](): Fu[List[A]] =
      gather[A, List](Int.MaxValue)

    def list[A: BSONDocumentReader](limit: Int): Fu[List[A]] =
      gather[A, List](limit)

    def list[A: BSONDocumentReader](limit: Option[Int]): Fu[List[A]] =
      gather[A, List](limit | Int.MaxValue)

    def list[A: BSONDocumentReader](limit: Option[Int], readPreference: ReadPreference): Fu[List[A]] =
      gather[A, List](limit | Int.MaxValue, readPreference)

    def list[A: BSONDocumentReader](limit: Int, readPreference: ReadPreference): Fu[List[A]] =
      gather[A, List](limit, readPreference)

    def vector[A: BSONDocumentReader](limit: Int, readPreference: ReadPreference): Fu[Vector[A]] =
      gather[A, Vector](limit, readPreference)
  }
   */
}
