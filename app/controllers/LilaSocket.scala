package controllers

import play.api.http._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._, Results._
import play.api.mvc.WebSocket.FrameFormatter

import lila.api.{ Context, TokenBucket }
import lila.app._
import lila.common.HTTPRequest

trait LilaSocket { self: LilaController =>

  private type AcceptType[A] = Context => Fu[Either[Result, (Iteratee[A, _], Enumerator[A])]]

  private val logger = lila.log("ratelimit")

  def rateLimitedSocket[A: FrameFormatter](consumer: TokenBucket.Consumer, name: String)(f: AcceptType[A]): WebSocket[A, A] =
    WebSocket[A, A] { req =>
      reqToCtx(req) flatMap { ctx =>
        val ip = HTTPRequest lastRemoteAddress req
        def userInfo = {
          val sri = get("sri", req) | "none"
          val username = ctx.usernameOrAnon
          s"user:$username sri:$sri"
        }
        // logger.debug(s"socket:$name socket connect $ip $userInfo")
        f(ctx).map { resultOrSocket =>
          resultOrSocket.right.map {
            case (readIn, writeOut) => (e, i) => {
              writeOut |>> i
              e &> Enumeratee.mapInputM { in =>
                consumer(ip).map { credit =>
                  if (credit >= 0) in
                  else {
                    logger.info(s"socket:$name socket close $ip $userInfo $in")
                    Input.EOF
                  }
                }
              } |>> readIn
            }
          }
        }
      }
    }
}
