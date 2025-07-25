package lila.core.lilaism

trait CoreExports:

  type Fu[A] = Future[A]
  type Funit = Fu[Unit]
  type PairOf[A] = (A, A)
  type Update[A] = A => A

  export scala.concurrent.{ ExecutionContextExecutor as Executor, Future, Promise }
  export scala.concurrent.duration.{ DurationInt, DurationLong, IntMult, Duration, FiniteDuration }
  export akka.actor.Scheduler
  export java.time.{ Instant, LocalDateTime }

  export scalalib.newtypes.{ given, * }
  export scalalib.zeros.given
  export scalalib.extensions.{ given, * }
  export scalalib.json.extensions.*
  export scalalib.json.Json.given_Zero_JsObject
  export scalalib.time.*
  export scalalib.model.{ Max, MaxPerPage, MaxPerSecond }

  export cats.syntax.all.*
  export cats.{ Eq, Show }
  export cats.data.NonEmptyList

object Core extends CoreExports
