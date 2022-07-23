package lila.db

import akka.actor.Scheduler
import alleycats.Zero
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

import lila.common.config.CollName
import lila.db.dsl._

final class AsyncColl(val name: CollName, resolve: () => Fu[Coll])(implicit ec: ExecutionContext) {

  def get: Fu[Coll] = resolve()

  def apply[A](f: Coll => Fu[A]) = get flatMap f

  def map[A](f: Coll => A): Fu[A] = get map f

  def failingSilently(timeout: FiniteDuration = 500 millis)(implicit scheduler: Scheduler) =
    new AsyncCollFailingSilently(this, timeout)
}

/* For data we don't really care about,
 * this DB coll with fallback to default when any operation fails. */
final class AsyncCollFailingSilently(coll: AsyncColl, timeout: FiniteDuration)(implicit
    ec: ExecutionContext,
    scheduler: Scheduler
) {

  def apply[A](f: Coll => Fu[A])(implicit default: Zero[A]) =
    coll.get
      .withTimeout(timeout)
      .transformWith {
        case Failure(_) => fuccess(default.zero)
        case Success(c) => f(c)
      }
}
