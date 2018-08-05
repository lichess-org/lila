package lila.db

import scala.concurrent.{ ExecutionContext, Future }

import scala.collection.generic.CanBuildFrom

import com.github.ghik.silencer.silent

import reactivemongo.api.Cursor
import reactivemongo.core.protocol.Response

final class CursorExt[T] private[db] (underlying: Cursor[T]) extends Cursor[T] {
  import Cursor.{ ErrorHandler, FailOnError, State }

  // Extensions

  def list(limit: Option[Int]): Fu[List[T]] =
    underlying.collect[List](limit.getOrElse(-1), Cursor.ContOnError[List[T]]())

  def list(limit: Int): Fu[List[T]] =
    underlying.collect[List](limit, Cursor.ContOnError[List[T]]())

  def list(): Fu[List[T]] =
    underlying.collect[List](-1, Cursor.ContOnError[List[T]]())

  def vector(limit: Int): Fu[Vector[T]] =
    underlying.collect[Vector](limit, Cursor.ContOnError[Vector[T]]())

  // ---

  def collect[M[_]](maxDocs: Int, err: ErrorHandler[M[T]])(implicit cbf: CanBuildFrom[M[_], T, M[T]], ec: ExecutionContext): Future[M[T]] = underlying.collect(maxDocs, err)(cbf, ec)

  @silent // Response type will be private
  def foldResponses[A](z: => A, maxDocs: Int = -1)(suc: (A, Response) => State[A], err: ErrorHandler[A] = FailOnError[A]())(implicit ctx: ExecutionContext): Future[A] = underlying.foldResponses[A](z, maxDocs)(suc, err)(ctx)

  @silent // Response type will be private
  def foldResponsesM[A](z: => A, maxDocs: Int = -1)(suc: (A, Response) => Future[State[A]], err: ErrorHandler[A] = FailOnError[A]())(implicit ctx: ExecutionContext): Future[A] = underlying.foldResponsesM[A](z, maxDocs)(suc, err)(ctx)

  def foldBulks[A](z: => A, maxDocs: Int = -1)(suc: (A, Iterator[T]) => State[A], err: ErrorHandler[A] = FailOnError[A]())(implicit ctx: ExecutionContext): Future[A] = underlying.foldBulks[A](z, maxDocs)(suc, err)(ctx)

  def foldBulksM[A](z: => A, maxDocs: Int = -1)(suc: (A, Iterator[T]) => Future[State[A]], err: ErrorHandler[A] = FailOnError[A]())(implicit ctx: ExecutionContext): Future[A] = underlying.foldBulksM[A](z, maxDocs)(suc, err)(ctx)

  def foldWhile[A](z: => A, maxDocs: Int = -1)(suc: (A, T) => State[A], err: ErrorHandler[A] = FailOnError[A]())(implicit ctx: ExecutionContext): Future[A] = underlying.foldWhile[A](z, maxDocs)(suc, err)(ctx)

  def foldWhileM[A](z: => A, maxDocs: Int = -1)(suc: (A, T) => Future[State[A]], err: ErrorHandler[A] = FailOnError[A]())(implicit ctx: ExecutionContext): Future[A] = underlying.foldWhileM[A](z, maxDocs)(suc, err)(ctx)

  def head(implicit ctx: ExecutionContext): Future[T] = underlying.head(ctx)

  def headOption(implicit ctx: ExecutionContext): Future[Option[T]] =
    underlying.headOption(ctx)

}
