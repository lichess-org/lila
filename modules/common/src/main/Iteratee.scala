package lila.common

import akka.actor.ActorSystem
import play.api.libs.iteratee._
import scala.concurrent.duration._

object Iteratee {

  def delay[A](duration: FiniteDuration)(implicit system: ActorSystem): Enumeratee[A, A] =
    Enumeratee.mapM[A].apply[A] { as =>
      lila.common.Future.delay[A](duration)(fuccess(as))
    }

  // Avoid running `andThen` on empty elements, just for perf
  def prepend[A](elements: Seq[A], enumerator: Enumerator[A]): Enumerator[A] =
    if (elements.isEmpty) enumerator
    else Enumerator(elements: _*) >>> enumerator

  // Avoid running `andThen` on empty elements, just for perf
  def prependFu[A](elements: Fu[Seq[A]], enumerator: Enumerator[A]): Enumerator[A] =
    Enumerator flatten {
      elements map { prepend(_, enumerator) }
    }
}
