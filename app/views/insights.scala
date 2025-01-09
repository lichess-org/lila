package views.html

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

object insights {

  def apply(user: User, path: String)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${user.username} - ${trans.insights.insights.txt()}",
      moreCss = cssTag("insights"),
      moreJs = frag(
        moduleJsTag(
          "insights",
          Json.obj(
            "username" -> user.username,
            "usernameHash" -> MessageDigest
              .getInstance("MD5")
              .digest(
                (insightsSecret + user.id).getBytes(UTF_8)
              )
              .map("%02x".format(_))
              .mkString,
            "isBot"    -> user.isBot,
            "path"     -> path,
            "endpoint" -> insightsEndpoint
          )
        )
      ),
      robots = false
    ) {
      main(id   := "insights-app")(
        div(cls := "insights-app--wrap")
      )
    }

  def privated(user: User)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${user.username} - ${trans.insights.insights.txt()}",
      moreCss = cssTag("insights"),
      robots = false
    ) {
      main(id   := "insights-app")(
        div(cls := "page-menu__menu"),
        div(cls := "page-menu__content")(
          h1(cls := "text")(trans.isPrivate.txt())
        )
      )
    }
}
