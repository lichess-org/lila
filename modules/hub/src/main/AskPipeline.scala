package lila.hub

import com.github.blemale.scaffeine.LoadingCache
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Promise }

/*
 * Only processes one computation at a time
 * and only enqueues one.
 */
final class AskPipeline[A](compute: () => Fu[A], timeout: FiniteDuration, name: String)(implicit
    system: akka.actor.ActorSystem,
    ec: scala.concurrent.ExecutionContext
) extends SyncActor {

  private var state: State = Idle

  protected val process: SyncActor.Receive = {

    case Get(promise) =>
      state match {
        case Idle =>
          startComputing()
          state = Processing(List(promise), Nil)
        case p @ Processing(_, next) =>
          state = p.copy(next = promise :: next)
      }

    case Done(res) =>
      complete(Right(res))

    case Fail(err) =>
      lila.log("hub").warn(name, err)
      complete(Left(err))
  }

  def get: Fu[A] = ask[A](Get.apply)

  private def startComputing() =
    compute()
      .withTimeout(timeout)
      .addEffects(
        err => this ! Fail(err),
        res => this ! Done(res)
      )

  private def complete(res: Either[Exception, A]) =
    state match {
      case Idle => // ???
      case Processing(current, next) =>
        res.fold(
          err => current.foreach(_ failure err),
          res => current.foreach(_ success res)
        )
        if (next.isEmpty) state = Idle
        else {
          startComputing()
          state = Processing(next, Nil)
        }
    }

  private case class Get(promise: Promise[A])
  private case class Done(result: A)
  private case class Fail(err: Exception)

  sealed private trait State
  private case object Idle                                                         extends State
  private case class Processing(current: List[Promise[A]], next: List[Promise[A]]) extends State
}

// Distributes tasks to many pipelines
final class AskPipelines[K, R](
    compute: K => Fu[R],
    expiration: FiniteDuration,
    timeout: FiniteDuration,
    name: String
)(implicit
    ec: ExecutionContext,
    system: akka.actor.ActorSystem,
    mode: play.api.Mode
) {

  def apply(key: K): Fu[R] = pipelines.get(key).get

  private val pipelines: LoadingCache[K, AskPipeline[R]] =
    lila.common.LilaCache
      .scaffeine(mode)
      .expireAfterAccess(expiration)
      .build(key => new AskPipeline[R](() => compute(key), timeout, name = s"$name:$key"))
}
