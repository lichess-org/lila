package controllers

import play.api.http._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._, Results._
import play.api.mvc.WebSocket.FrameFormatter

import lila.api.TokenBucketGroup
import lila.app._
import lila.common.HTTPRequest

object LilaSocket {

  private type AcceptType[A] = RequestHeader => Fu[Either[Result, (Iteratee[A, _], Enumerator[A])]]

  private val logger = play.api.Logger("ratelimit")

  def rateLimited[A: FrameFormatter](consumer: TokenBucketGroup.Consumer)(f: AcceptType[A]): WebSocket[A, A] =
    WebSocket[A, A] { req =>
      val ip = HTTPRequest lastRemoteAddress req
      f(req).map { resultOrSocket =>
        resultOrSocket.right.map {
          case (readIn, writeOut) => (e, i) => {
            writeOut |>> i
            e &> Enumeratee.mapInputM { in =>
              consumer(ip).map { credit =>
                if (credit >= 0) in
                else {
                  logger.info(s"socket close $ip $in")
                  Input.EOF
                }
              }
            } |>> readIn
          }
        }
      }
    }
}
