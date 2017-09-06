package controllers

import akka.stream.scaladsl._
import play.api.libs.iteratee._
import play.api.mvc._
// import play.api.mvc.WebSocket.FrameFormatter
import play.api.http.websocket.{ Message => Msg }
import play.api.libs.json._

import lila.api.Context
import lila.app._
import lila.common.{ HTTPRequest, IpAddress }

trait LilaSocket { self: LilaController =>

  private type IterateeWs = (Iteratee[JsValue, _], Enumerator[JsValue])
  private type FlowWs = Flow[JsValue, JsValue, _]
  private type AcceptType = Context => Fu[Either[Result, IterateeWs]]

  private val notFoundResponse = NotFound(jsonError("socket resource not found"))

  // protected def SocketEither[A: FrameFormatter](f: Context => Fu[Either[Result, Pipe[A]]]) =
  protected def SocketEither(f: AcceptType) =
    WebSocket.acceptOrResult[JsValue, JsValue] { req =>
      SocketCSRF(req) {
        reqToCtx(req) flatMap { ctx =>
          f(ctx).map(_.right map iterateeToFlow)
        }
      }
    }

  private def iterateeToFlow(i: IterateeWs): FlowWs = iterateeToSinkAndSource(i) match {
    case (sink, source) => Flow.fromSinkAndSource(sink, source)
    // .watchTermination()((_, f) => f.onComplete {
    //   case cause => println(s"WS stream terminated $cause")
    // })
  }

  private def iterateeToSinkAndSource(i: IterateeWs) = i match {
    case (iteratee, enumerator) =>
      import play.api.libs.iteratee.streams.IterateeStreams
      val publisher = IterateeStreams.enumeratorToPublisher(enumerator)
      val (subscriber, _) = IterateeStreams.iterateeToSubscriber(iteratee)
      Sink.fromSubscriber(subscriber) -> Source.fromPublisher(publisher)
  }

  // protected def Socket[A: FrameFormatter](f: Context => Fu[Pipe[A]]) =
  protected def Socket(f: Context => Fu[IterateeWs]) =
    SocketEither { ctx =>
      f(ctx) map scala.util.Right.apply
    }

  // protected def SocketOption[A: FrameFormatter](f: Context => Fu[Option[Pipe[A]]]) =
  protected def SocketOption(f: Context => Fu[Option[IterateeWs]]) =
    SocketEither { ctx =>
      f(ctx).map(_ toRight notFoundResponse)
    }

  protected def SocketOptionLimited(limiter: lila.memo.RateLimit[IpAddress], name: String)(f: Context => Fu[Option[IterateeWs]]) =
    rateLimitedSocket(limiter, name) { ctx =>
      f(ctx).map(_ toRight notFoundResponse)
    }

  private val rateLimitLogger = lila.log("ratelimit")

  private def rateLimitedSocket(limiter: lila.memo.RateLimit[IpAddress], name: String)(f: AcceptType) =
    WebSocket.acceptOrResult[JsValue, JsValue] { req =>
      SocketCSRF(req) {
        reqToCtx(req) flatMap { ctx =>
          f(ctx).map { resultOrSocket =>
            resultOrSocket.right map iterateeToSinkAndSource map {
              case (sink, source) =>
                val canPass = limiter[Boolean](HTTPRequest lastRemoteAddress req, 1) _
                val limiterFlow = Flow[JsValue].filter { _ => canPass(true) }
                Flow.fromSinkAndSource(
                  limiterFlow to sink,
                  source
                )
            }
          }
        }
      }
    }

  private def SocketCSRF[A](req: RequestHeader)(f: => Fu[Either[Result, A]]): Fu[Either[Result, A]] =
    if (csrfCheck(req)) f else csrfForbiddenResult map Left.apply
}
