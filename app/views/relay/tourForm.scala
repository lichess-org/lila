package views.html.relay

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.relay.RelayTourForm.Data
import lila.relay.RelayTour

object tourForm:

  import trans.broadcast.*

  def create(form: Form[Data])(implicit ctx: Context) =
    layout(newBroadcast.txt(), menu = "new".some)(
      boxTop(h1(newBroadcast())),
      postForm(cls := "form3", action := routes.RelayTour.create)(
        inner(form),
        form3.actions(
          a(href := routes.RelayTour.index(1))(trans.cancel()),
          form3.submit(trans.apply())
        )
      )
    )

  def edit(t: RelayTour, form: Form[Data])(implicit ctx: Context) =
    layout(t.name, menu = none)(
      boxTop(h1("Edit ", a(href := routes.RelayTour.redirectOrApiTour(t.slug, t.id.value))(t.name))),
      postForm(cls := "form3", action := routes.RelayTour.update(t.id.value))(
        inner(form),
        form3.actions(
          a(href := routes.RelayTour.redirectOrApiTour(t.slug, t.id.value))(trans.cancel()),
          form3.submit(trans.apply())
        )
      )
    )

  private def layout(title: String, menu: Option[String])(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("relay.form")
    )(menu match {
      case Some(active) =>
        main(cls := "page-small page-menu")(
          tour.pageMenu(active),
          div(cls := "page-menu__content box box-pad")(body)
        )
      case None => main(cls := "page-small box box-pad")(body)
    })

  private def inner(form: Form[Data])(implicit ctx: Context) = frag(
    div(cls := "form-group")(bits.howToUse),
    form3.globalError(form),
    form3.group(form("name"), tournamentName())(form3.input(_)(autofocus)),
    form3.group(form("description"), tournamentDescription())(form3.textarea(_)(rows := 2)),
    form3.group(
      form("markdown"),
      fullDescription(),
      help = fullDescriptionHelp(
        a(
          href := "https://guides.github.com/features/mastering-markdown/",
          targetBlank
        )("Markdown"),
        20000.localize
      ).some
    )(form3.textarea(_)(rows := 10)),
    form3.split(
      form3.checkbox(
        form("autoLeaderboard"),
        raw("Automatic leaderboard"),
        help = raw("Compute and display a simple leaderboard based on game results").some,
        half = true
      ),
      if (isGranted(_.Relay))
        form3.group(
          form("tier"),
          raw("Official Lichess broadcast tier"),
          help = raw("Feature on /broadcast - for admins only").some,
          half = true
        )(form3.select(_, RelayTour.Tier.options))
      else form3.hidden(form("tier"))
    ),
    form3.group(
      form("players"),
      "Optional: replace player names and ratings",
      help = frag(
        "One line per player, formatted as such:",
        pre("Original name; Replacement name; Optional replacement rating"),
        "Example:",
        pre("""DrNykterstein;Magnus Carlsen;2863
AnishGiri;Anish Giri;2764""")
      ).some
    )(form3.textarea(_)(rows := 3))
  )
