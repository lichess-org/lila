package lila.web

import play.api.mvc.RequestHeader
import play.api.data.Form

import lila.core.security.LilaCookie
import lila.core.id.SessionId

final class T3AuthMonitor(using Executor):

  val login = AuthPage("login")
  val signup = AuthPage("signup")

  final class AuthPage(name: String):

    private val mon = lila.mon.signedClient.AuthPage(name)

    private val hasLoaded = scalalib.cache.ExpireSetMemo[SessionId](1.hour)
    private val hasFailed = scalalib.cache.ExpireSetMemo[SessionId](1.hour)

    def load(sid: SessionId)(client: String): Unit =
      mon.load(unique = false)(client).increment()
      if !hasLoaded.get(sid) then mon.load(unique = true)(client).increment()
      hasLoaded.put(sid)

    def fail(reason: String, formErr: Option[Form[?]] = none)(
        client: String
    )(using req: RequestHeader): Unit =
      for sid <- LilaCookie.sid(req)
      do
        formErr.foreach: f =>
          mon
            .formError(listStr(f.errors.map(_.key)), listStr(f.errors.flatMap(_.messages.headOption)))(client)
            .increment()
          mon.failure(reason, unique = false)(client).increment()
        if !hasFailed.get(sid) then mon.failure(reason, unique = true)(client).increment()
        hasFailed.put(sid)

    def success(client: String)(using req: RequestHeader): Unit =
      mon.success(hasFailed = LilaCookie.sid(req).exists(hasFailed.get))(client).increment()

    def step(s: String)(client: String): Unit =
      mon.step(s)(client).increment()

    private def listStr(list: Seq[String]): String = list.mkString(",").nonEmptyOption | "-"
