package lila.db

import alleycats.Zero

import scala.util.{ Failure, Success }

import lila.core.config.CollName
import lila.db.dsl.*

final class AsyncColl(val name: CollName, resolve: () => Fu[Coll])(using Executor):

  def get: Fu[Coll] = resolve()

  def apply[A](f: Coll => Fu[A]) = get.flatMap(f)

  def map[A](f: Coll => A): Fu[A] = get.map(f)

  def failingSilently(timeout: FiniteDuration = 500.millis)(using Scheduler) =
    AsyncCollFailingSilently(this, timeout)

/* For data we don't really care about,
 * this DB coll with fallback to default when any operation fails. */
final class AsyncCollFailingSilently(coll: AsyncColl, timeout: FiniteDuration)(using Executor, Scheduler)
    extends lila.core.db.AsyncCollFailingSilently:

  def apply[A](f: Coll => Fu[A])(using default: Zero[A]): Fu[A] =
    coll.get
      .withTimeout(timeout, coll.name.value)
      .transformWith:
        case Failure(_) => fuccess(default.zero)
        case Success(c) => f(c)
