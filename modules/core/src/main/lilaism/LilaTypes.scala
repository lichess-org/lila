package lila.core.lilaism

import alleycats.Zero
import scalalib.newtypes.*
import play.api.libs.json.JsObject

trait LilaTypes:

  type Fu[A]     = Future[A]
  type Funit     = Fu[Unit]
  type PairOf[A] = (A, A)

  export scala.concurrent.{ ExecutionContextExecutor as Executor, Future, Promise }
  export scala.concurrent.duration.{ DurationInt, DurationLong, IntMult, Duration, FiniteDuration }
  export akka.actor.Scheduler
  export java.time.{ Instant, LocalDateTime }

  def fuccess[A](a: A): Fu[A]        = Future.successful(a)
  def fufail[X](t: Throwable): Fu[X] = Future.failed(t)
  def fufail[X](s: String): Fu[X]    = fufail(LilaException(s))
  val funit                          = Future.unit
  val fuTrue                         = fuccess(true)
  val fuFalse                        = fuccess(false)
