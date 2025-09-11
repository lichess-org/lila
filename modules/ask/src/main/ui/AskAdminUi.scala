package lila.ask
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.ask.Ask

final class AskAdminUi(helpers: Helpers)(askRender: (Ask) => Context ?=> Frag):
  import helpers.{ *, given }

  def show(asks: List[Ask], user: lila.core.LightUser)(using Me, Context) =
    val askmap = asks.sortBy(_.createdAt).groupBy(_.url)
    Page(s"${user.titleName} asks")
      .css("bits.ask")
      .js(esmInit("bits.ask")):
        main(cls := "page-small box box-pad")(
          h1(s"${user.titleName} asks"),
          askmap.keys.map(url => showAsks(url, askmap.get(url).get)).toSeq
        )

  def showAsks(urlopt: Option[String], asks: List[Ask])(using Me, Context) =
    val groupActions = urlopt
      .filter(_ => asks.sizeIs > 1)
      .map: url =>
        div(cls := "url-actions")(
          a(href := routes.Ask.jsonByUrl(url))("JSON"),
          button(formaction := routes.Ask.resetByUrl(url))("Reset"),
          button(formaction := routes.Ask.concludeByUrl(url))("Conclude")
        )
    div(
      hr,
      h2(cls := "ask-group-header")(
        urlopt.fold(frag("no url"))(url => a(href := url)(url)),
        groupActions
      ),
      br,
      asks.map(renderOne)
    )

  def renderOne(ask: Ask)(using Context)(using me: Me) =
    div(cls := "ask-admin")(
      a(name := ask._id),
      div(cls := "header")(
        ask.question,
        div(cls := "url-actions")(
          button(formaction := routes.Ask.delete(ask._id))("Delete"),
          button(formaction := routes.Ask.reset(ask._id))("Reset"),
          (!ask.isConcluded).option(button(formaction := routes.Ask.conclude(ask._id))("Conclude")),
          a(href := routes.Ask.json(ask._id))("JSON")
        )
      ),
      div(cls := "inset")(
        Granter.opt(_.ModerateForum).option(property("id:", ask._id.value)),
        (!me.is(ask.creator)).option(property("creator:", ask.creator.value)),
        property("created at:", showInstant(ask.createdAt)),
        ask.tags.nonEmpty.option(property("tags:", ask.tags.mkString(", "))),
        ask.picks.map(p => (p.size > 0).option(property("responses:", p.size.toString))),
        p,
        askRender(ask)
      ),
      ask.form.map: fbmap =>
        frag(
          property("form respondents:", fbmap.size.toString),
          div(cls := "inset-box")(
            fbmap.toSeq.map:
              case (uid, fb) if uid.startsWith("anon-") => p(s"anon: $fb")
              case (uid, fb) => p(s"$uid: $fb")
          )
        )
    )

  def property(name: String, value: String) =
    div(cls := "prop")(div(cls := "name")(name), div(cls := "value")(value))
