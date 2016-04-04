package lila.common

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import org.reactivestreams.Publisher

object AkkaStream {

  def actorPublisher[T](
    bufferSize: Int,
    overflowStrategy: OverflowStrategy)(implicit materializer: Materializer): (ActorRef, Publisher[T]) = {
    Source.actorRef[T](20, OverflowStrategy.dropHead)
      .toMat(Sink asPublisher false)(Keep.both).run()
  }

  def actorSource[T](
    bufferSize: Int,
    overflowStrategy: OverflowStrategy)(implicit materializer: Materializer): (ActorRef, Source[T, _]) = {
    val (out, publisher) = actorPublisher(bufferSize, overflowStrategy)
    (out, Source fromPublisher publisher)
  }
}
