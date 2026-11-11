package lila.web

import lila.common.HTTPRequest

trait RequestGetter:

  export HTTPRequest.queryStringGet as get
  export HTTPRequest.queryStringBool as getBool
  export HTTPRequest.queryStringBoolOpt as getBoolOpt
  export HTTPRequest.queryStringGetAs as getAs

  protected def getUserStr(name: String)(using Req): Option[UserStr] =
    get(name).flatMap(UserStr.read)

  protected def getInt(name: String)(using req: Req) =
    req.queryString.get(name).flatMap(_.headOption).flatMap(_.toIntOption)

  protected def getIntAs[A](name: String)(using
      req: Req,
      sr: SameRuntime[Int, A]
  ): Option[A] =
    getInt(name).map(sr.apply)

  protected def getLong(name: String)(using Req) =
    get(name).flatMap(_.toLongOption)

  protected def getTimestamp(name: String)(using Req): Option[Instant] =
    getLong(name).map(millisToInstant)

  protected def getBoolAs[A](name: String)(using req: Req, yn: SameRuntime[Boolean, A]): A =
    yn(getBool(name))

  protected def getBoolOptAs[A](
      name: String
  )(using req: Req, yn: SameRuntime[Boolean, A]): Option[A] =
    getBoolOpt(name).map(yn.apply)

  protected def getColor(name: String = "color")(using Req): Option[Color] =
    get(name).flatMap(Color.fromName)
