package views.html

import controllers.routes
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.ask.Ask
import views.html.ask.*

object askAdmin:

  def show(asks: List[Ask], user: lila.common.LightUser)(using Me, PageContext) =
    views.html.base.layout(
      title = s"${user.titleName} polls",
      moreJs = jsModuleInit("ask"),
      moreCss = cssTag("ask"),
      csp = defaultCsp.withInlineIconFont.some
    ):
      val askmap = asks.sortBy(_.createdAt).groupBy(_.url)
      main(cls := "page-small box box-pad")(
        h1(s"${user.titleName} polls"),
        askmap.keys.map(url => showAsks(url, askmap.get(url).get)) toSeq
      )

  def showAsks(urlopt: Option[String], asks: List[Ask])(using Me, Context) =
    div(
      hr,
      h2(
        urlopt match
          case Some(url) => div(a(href := url)(url))
          case None      => "no url"
      ),
      br,
      asks map renderOne
    )

  def renderOne(ask: Ask)(using Context)(using me: Me) =
    div(cls := "ask-admin")(
      a(name := ask._id),
      div(cls := "header")(
        ask.question,
        div(cls := "url-actions")(
          button(formaction := routes.Ask.delete(ask._id))("Delete"),
          button(formaction := routes.Ask.reset(ask._id))("Reset"),
          !ask.isConcluded option button(formaction := routes.Ask.conclude(ask._id))("Conclude"),
          a(href := routes.Ask.json(ask._id))("JSON")
        )
      ),
      div(cls := "inset")(
        isGranted(_.ModerateForum) option property("id:", ask._id),
        !me.is(ask.creator) option property("creator:", ask.creator),
        property("created at:", showInstant(ask.createdAt)),
        ask.tags.nonEmpty option property("tags:", ask.tags.mkString(", ")),
        p,
        renderGraph(ask)
      ),
      frag:
        ask.form.map: fbmap =>
          div(cls := "inset-box")(
            fbmap.toSeq map:
              case (uid, fb) if uid.startsWith("anon-") => p(s"anon: $fb")
              case (uid, fb)                            => p(s"$uid: $fb")
          )
    )

  def property(name: String, value: String) =
    div(cls := "prop")(div(cls := "name")(name), div(cls := "value")(value))
