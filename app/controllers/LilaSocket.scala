package controllers

import play.api.libs.iteratee._
import play.api.mvc._
import play.api.mvc.WebSocket.FrameFormatter

import lila.api.Context
import lila.app._
import lila.common.{ HTTPRequest, IpAddress }
import lila.socket.Socket.SocketVersion

trait LilaSocket { self: LilaController =>

  private type Pipe[A] = (Iteratee[A, _], Enumerator[A])

  private val notFoundResponse = NotFound(jsonError("socket resource not found"))

  protected def getSocketVersion(implicit ctx: Context) = getInt("v") map SocketVersion.apply

  protected def SocketEither[A: FrameFormatter](f: Context => Fu[Either[Result, Pipe[A]]]): WebSocket[A, A] =
    WebSocket.tryAccept[A] { req =>
      SocketCSRF(req) {
        reqToCtx(req) flatMap f
      }
    }

  protected def Socket[A: FrameFormatter](f: Context => Fu[Pipe[A]]) =
    SocketEither[A] { ctx =>
      f(ctx) map scala.util.Right.apply
    }

  protected def SocketOption[A: FrameFormatter](f: Context => Fu[Option[Pipe[A]]]) =
    SocketEither[A] { ctx =>
      f(ctx).map(_ toRight notFoundResponse)
    }

  protected def SocketOptionLimited[A: FrameFormatter](limiter: lila.memo.RateLimit[IpAddress], name: String)(f: Context => Fu[Option[Pipe[A]]]) =
    rateLimitedSocket[A](limiter, name) { ctx =>
      f(ctx).map(_ toRight notFoundResponse)
    }

  private type AcceptType[A] = Context => Fu[Either[Result, Pipe[A]]]

  private val rateLimitLogger = lila.log("ratelimit")

  private def rateLimitedSocket[A: FrameFormatter](limiter: lila.memo.RateLimit[IpAddress], name: String)(f: AcceptType[A]): WebSocket[A, A] =
    WebSocket[A, A] { req =>
      SocketCSRF(req) {
        reqToCtx(req) flatMap { ctx =>
          val ip = HTTPRequest lastRemoteAddress req
          def userInfo = {
            val sri = get("sri", req) | "none"
            val username = ctx.usernameOrAnon
            s"user:$username sri:$sri"
          }
          f(ctx).map { resultOrSocket =>
            resultOrSocket.right.map {
              case (readIn, writeOut) => (e: Enumerator[A], i: Iteratee[A, Unit]) => {
                writeOut |>> i
                e &> Enumeratee.mapInput { in =>
                  if (limiter(ip, 1)(true)) in
                  else {
                    rateLimitLogger.info(s"socket:$name socket close $ip $userInfo $in")
                    Input.EOF
                  }
                } |>> readIn
                ()
              }
            }
          }
        }
      }
    }

  private def SocketCSRF[A](req: RequestHeader)(f: => Fu[Either[Result, A]]): Fu[Either[Result, A]] =
    if (csrfCheck(req)) f else csrfForbiddenResult map Left.apply
}
