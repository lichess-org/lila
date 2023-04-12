package lila.base

import ornicar.scalalib.newtypes.*

import alleycats.Zero
import play.api.libs.json.JsObject

trait LilaTypes:

  type Fu[A]     = Future[A]
  type Funit     = Fu[Unit]
  type PairOf[A] = (A, A)

  export scala.concurrent.{ ExecutionContext as Executor, Future, Promise }
  export scala.concurrent.duration.{ DurationInt, DurationLong, IntMult, Duration, FiniteDuration }
  export akka.actor.Scheduler
  export java.time.{ Instant, LocalDateTime }

  def fuccess[A](a: A): Fu[A]        = Future.successful(a)
  def fufail[X](t: Throwable): Fu[X] = Future.failed(t)
  def fufail[X](s: String): Fu[X]    = fufail(LilaException(s))
  val funit                          = fuccess(())
  val fuTrue                         = fuccess(true)
  val fuFalse                        = fuccess(false)

  given Zero[Funit] with
    def zero = funit
  given Zero[Fu[Boolean]] with
    def zero = fuFalse
  given [A](using az: Zero[A]): Zero[Fu[A]] with
    def zero = fuccess(az.zero)

  // given Zero[Duration] with
  //   def zero = Duration.Zero
  given Zero[JsObject] with
    def zero = JsObject(Seq.empty)

  given [A](using sr: SameRuntime[Boolean, A]): Zero[A] = Zero(sr(false))
