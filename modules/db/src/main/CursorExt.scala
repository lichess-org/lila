package lila.db

import scala.collection.generic.CanBuildFrom

import reactivemongo.api._
import reactivemongo.bson._

trait CursorExt { self: dsl =>

  // Can be refactor as CursorProducer
  final implicit class ExtendCursor[A: BSONDocumentReader](val c: Cursor[A]) {

    // like collect, but with stopOnError defaulting to false
    def gather[M[_]](upTo: Int = Int.MaxValue)(implicit cbf: CanBuildFrom[M[_], A, M[A]]): Fu[M[A]] = c.collect[M](upTo, Cursor.ContOnError[M[A]]())

    def list(limit: Option[Int]): Fu[List[A]] = gather[List](limit | Int.MaxValue)

    def list(limit: Int): Fu[List[A]] = list(limit.some)

    def list(): Fu[List[A]] = list(none)

    // like headOption, but with stopOnError defaulting to false
    def uno: Fu[Option[A]] = c.collect[Iterable](
      1, Cursor.ContOnError[Iterable[A]]()).map(_.headOption)
  }
}
