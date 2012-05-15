package lila
package socket

import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

// Copy of PushEnumerator, but with initial messages
// Keep uptodate with framework/src/play/src/main/scala/play/api/libs/iteratee/Iteratee.scala PushEnumerator (l.1020)
final class LilaEnumerator[E](
    in: List[E],
    onStart: () => Unit = () => (),
    onComplete: () => Unit = () => (),
    onError: (String, Input[E]) => Unit = (_: String, _: Input[E]) => ())
extends Enumerator[E] with Enumerator.Pushee[E] {

  var iteratee: Iteratee[E, _] = _
  var promise: Promise[Iteratee[E, _]] with Redeemable[Iteratee[E, _]] = _

  def apply[A](it: Iteratee[E, A]): Promise[Iteratee[E, A]] = {
    onStart()
    iteratee = it.asInstanceOf[Iteratee[E, _]]
    in foreach push
    val newPromise = new STMPromise[Iteratee[E, A]]()
    promise = newPromise.asInstanceOf[Promise[Iteratee[E, _]] with Redeemable[Iteratee[E, _]]]
    newPromise
  }

  def close() {
    if (iteratee != null) {
      iteratee.feed(Input.EOF).map(result => promise.redeem(result))
      iteratee = null
      promise = null
    }
  }

  def push(item: E): Boolean = {
    if (iteratee != null) {
      iteratee = iteratee.pureFlatFold[E, Any] {

        case Step.Done(a, in) => {
          onComplete()
          Done(a, in)
        }

        case Step.Cont(k) => {
          val next = k(Input.El(item))
          next.pureFlatFold {
            case Step.Done(a, in) => {
              onComplete()
              next
            }
            case _ => next
          }
        }

        case Step.Error(e, in) => {
          onError(e, in)
          Error(e, in)
        }
      }
      true
    } else {
      false
    }
  }

}
