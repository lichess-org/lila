package lila.db

import scala.collection.Factory

import scala.concurrent.ExecutionContext

import reactivemongo.api._

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
}
