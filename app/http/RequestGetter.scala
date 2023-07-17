package lila.app
package http

import play.api.mvc.RequestHeader

import lila.common.Form.trueish
import lila.common.HTTPRequest

trait RequestGetter:

  private given (using ctx: Context): RequestHeader = ctx.req

  protected def get(name: String)(using req: RequestHeader): Option[String] =
    HTTPRequest.queryStringGet(req, name)

  protected def getAs[A](name: String)(using
      req: RequestHeader,
      sr: SameRuntime[String, A]
  ): Option[A] =
    get(name).map(sr.apply)

  protected def getUserStr(name: String)(using RequestHeader): Option[UserStr] =
    get(name) flatMap UserStr.read

  protected def getInt(name: String)(using req: RequestHeader) =
    req.queryString get name flatMap (_.headOption) flatMap (_.toIntOption)

  protected def getIntAs[A](name: String)(using
      req: RequestHeader,
      sr: SameRuntime[Int, A]
  ): Option[A] =
    getInt(name).map(sr.apply)

  protected def getLong(name: String)(using RequestHeader) =
    get(name).flatMap(_.toLongOption)

  protected def getTimestamp(name: String)(using RequestHeader) =
    getLong(name) map millisToInstant

  protected def getBool(name: String)(using RequestHeader): Boolean =
    (getInt(name) exists trueish) || (get(name) exists trueish)

  protected def getBoolOpt(name: String)(using RequestHeader): Option[Boolean] =
    getInt(name).map(trueish) orElse get(name).map(trueish)
