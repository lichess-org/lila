package views.html

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.ask.Ask
import views.html.ask._

// thrown together, half-baked, prototype code
object askAdmin {

  import RenderType._

  def show(asks: List[Ask], user: lila.common.LightUser)(implicit ctx: Context): Frag =
    views.html.base.layout(
      title = s"${user.titleName} polls",
      moreJs = jsModule("ask"),
      moreCss = cssTag("ask"),
      csp = defaultCsp.withInlineIconFont.some
    ) {
      val askmap = asks.sortBy(_.createdAt).groupBy(_.url)
      main(cls := "page-small box box-pad")(
        h1(s"${user.titleName} polls"),
        askmap.keys.map(url => showAsks(url, askmap.get(url).get)) toSeq
      )
    }

  def showAsks(urlopt: Option[String], asks: List[Ask])(implicit ctx: Context) =
    div(
      hr,
      h2(
        urlopt match {
          case Some(url) => div(style := "text-align: center")(a(href := url)(url), p)
          case None      => "no url"
        }
      ),
      asks map renderInner
    )
  def renderInner(as: Ask)(implicit ctx: Context) = {
    div(cls := "ask-admin")(
      a(name := as._id),
      div(cls := "header")(
        as.question,
        div(cls := "actions")(
          button(cls := "action", tpe := "submit", formaction := routes.Ask.delete(as._id))("Delete"),
          button(cls := "action", tpe := "submit", formaction := routes.Ask.reset(as._id))("Reset"),
          button(cls := "action", tpe := "submit", formaction := routes.Ask.conclude(as._id))("Conclude")
        )
      ),
      div(cls := "inset")(
        property("id:", as._id),
        property("creator:", as.creator),
        property("created at:", as.createdAt.toString),
        property("tags:", as.tags.toString),
        p,
        ask.RenderType(as) match {
          case POLL | BAR | QUIZ => as.choices.nonEmpty ?? barGraphBody(as)
          case RANK | RANKBAR    => as.choices.nonEmpty ?? rankGraphBody(as)
        }
      ),
      as.feedback map { case fbmap =>
        div(cls := "inset-box")(
          fbmap.toSeq map { case (uid, fb) =>
            p(s"$uid: $fb")
          }
        )
      },
      div(cls := "actions")(button(tpe := "submit")("Download CSV file")),
      hr,
      p
    )
  }
  def property(name: String, value: String) =
    div(cls := "prop")(div(cls := "name")(name), div(cls := "value")(value))

  def mkString(as: Ask): String = {
    val sb = new StringBuilder();
    sb ++= s"question:\n  ${as.question}\n"
    if (as.choices.nonEmpty) sb ++= "choices:\n"
    as.choices foreach (c =>
      if (c == ~as.answer) sb ++= s" -> * $c\n"
      else sb ++= s"    * $c\n"
    )
    if (as.footer.isDefined) sb ++= s"footer:\n    ${~as.footer}\n"
    as.feedback match {
      case Some(fbmap) =>
        sb ++= "feedback:\n"
        fbmap map { case (uid, fb) =>
          sb ++= s"    $uid: $fb\n"
        }
      case None =>
        sb ++= "\n"
    }
    as.picks match {
      case Some(picks) =>
        sb ++= "picks:\n"
        picks map { case (uid, pick) =>
          sb ++= s"    $uid: ${pick.map(c => as.choices(c)).mkString(", ")}\n"
        }
      case None =>
        sb ++= "\n"
    }
    sb.toString
  }
}
