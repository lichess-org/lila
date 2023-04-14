package controllers

import play.api.mvc.RequestHeader

import lila.common.Form.trueish
import lila.common.HTTPRequest
import lila.user.UserContext
import lila.app.*

trait RequestGetter:

  protected def get(name: String)(using ctx: UserContext): Option[String] = get(name, ctx.req)
  protected def getAs[A](
      name: String
  )(using ctx: UserContext, sr: SameRuntime[String, A]): Option[A] = get(name).map(sr.apply)

  protected def get(name: String, req: RequestHeader): Option[String] =
    HTTPRequest.queryStringGet(req, name)

  protected def getAs[A](name: String, req: RequestHeader)(using
      sr: SameRuntime[String, A]
  ): Option[A] =
    get(name, req).map(sr.apply)

  protected def getUserStr(name: String)(using ctx: UserContext): Option[UserStr] =
    get(name, ctx.req) flatMap UserStr.read

  protected def getUserStr(name: String, req: RequestHeader): Option[UserStr] =
    get(name, req) flatMap UserStr.read

  protected def getInt(name: String)(using UserContext) =
    get(name) flatMap (_.toIntOption)

  protected def getInt(name: String, req: RequestHeader): Option[Int] =
    req.queryString get name flatMap (_.headOption) flatMap (_.toIntOption)

  protected def getIntAs[A](name: String, req: RequestHeader)(using
      sr: SameRuntime[Int, A]
  ): Option[A] =
    getInt(name, req).map(sr.apply)

  protected def getLong(name: String)(using UserContext) =
    get(name) flatMap (_.toLongOption)

  protected def getLong(name: String, req: RequestHeader) =
    get(name, req) flatMap (_.toLongOption)

  protected def getTimestamp(name: String, req: RequestHeader) =
    getLong(name, req) map millisToInstant

  protected def getBool(name: String)(using UserContext): Boolean =
    (getInt(name) exists trueish) || (get(name) exists trueish)

  protected def getBool(name: String, req: RequestHeader): Boolean =
    (getInt(name, req) exists trueish) || (get(name, req) exists trueish)

  protected def getBoolOpt(name: String)(using UserContext): Option[Boolean] =
    getInt(name).map(trueish) orElse get(name).map(trueish)

  protected def getBoolOpt(name: String, req: RequestHeader): Option[Boolean] =
    getInt(name, req).map(trueish) orElse get(name, req).map(trueish)
