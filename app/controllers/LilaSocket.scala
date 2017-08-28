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

  private val notFoundResponse = NotFound(jsonError("socket resource not found"))

  // protected def SocketEither[A: FrameFormatter](f: Context => Fu[Either[Result, Pipe[A]]]) =
  protected def SocketEither(f: Context => Fu[Either[Result, IterateeWs]]) =
    WebSocket.acceptOrResult[JsValue, JsValue] { req =>
      SocketCSRF(req) {
        reqToCtx(req) flatMap { ctx =>
          f(ctx).map(_.right map iterateeToFlow)
        }
      }
    }

  private def iterateeToFlow(i: IterateeWs): FlowWs = i match {
    case (iteratee, enumerator) =>
      import play.api.libs.iteratee.streams.IterateeStreams
      val publisher = IterateeStreams.enumeratorToPublisher(enumerator)
      val (subscriber, _) = IterateeStreams.iterateeToSubscriber(iteratee)
      Flow.fromSinkAndSource(
        Sink.fromSubscriber(subscriber),
        Source.fromPublisher(publisher)
      )
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

  // protected def SocketOptionLimited[A: FrameFormatter](limiter: lila.memo.RateLimit[IpAddress], name: String)(f: Context => Fu[Option[Pipe[A]]]) =
  protected def SocketOptionLimited(limiter: lila.memo.RateLimit[IpAddress], name: String)(f: Context => Fu[Option[IterateeWs]]) =
    rateLimitedSocket(limiter, name) { ctx =>
      f(ctx).map(_ toRight notFoundResponse)
    }

  private type AcceptType = Context => Fu[Either[Result, IterateeWs]]

  private val rateLimitLogger = lila.log("ratelimit")

  private def rateLimitedSocket(limiter: lila.memo.RateLimit[IpAddress], name: String)(f: AcceptType) =
    WebSocket.acceptOrResult[JsValue, JsValue] { req =>
      SocketCSRF(req) {
        reqToCtx(req) flatMap { ctx =>
          val ip = HTTPRequest lastRemoteAddress req
          def userInfo = {
            val sri = get("sri", req) | "none"
            val username = ctx.usernameOrAnon
            s"user:$username sri:$sri"
          }
          f(ctx).map { resultOrSocket =>
            // resultOrSocket.right.map {
            //   case (readIn, writeOut) => (e: Enumerator[JsValue], i: Iteratee[JsValue, _]) => {
            //     writeOut |>> i
            //     e &> Enumeratee.mapInput { in =>
            //       if (limiter(ip, 1)(true)) in
            //       else {
            //         rateLimitLogger.info(s"socket:$name socket close $ip $userInfo $in")
            //         Input.EOF
            //       }
            //     } |>> readIn
            //   }
            // } map iterateeToFlow
            resultOrSocket.right map iterateeToFlow
          }
        }
      }
    }

  private def SocketCSRF[A](req: RequestHeader)(f: => Fu[Either[Result, A]]): Fu[Either[Result, A]] =
    if (csrfCheck(req)) f else csrfForbiddenResult map Left.apply
}
