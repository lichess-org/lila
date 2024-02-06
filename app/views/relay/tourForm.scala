package views.html.relay

import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.relay.RelayTourForm.Data
import lila.relay.RelayTour

object tourForm:

  import trans.broadcast.*

  def create(form: Form[Data])(using PageContext) =
    layout(newBroadcast.txt(), menu = "new".some)(
      boxTop(h1(dataIcon := licon.RadioTower, cls := "text")(newBroadcast())),
      postForm(cls := "form3", action := routes.RelayTour.create)(
        inner(form),
        form3.actions(
          a(href := routes.RelayTour.index(1))(trans.cancel()),
          form3.submit(trans.apply())
        )
      )
    )

  def edit(t: RelayTour, form: Form[Data])(using PageContext) =
    layout(t.name, menu = none)(
      boxTop:
        h1(dataIcon := licon.Pencil, cls := "text"):
          a(href := routes.RelayTour.show(t.slug, t.id))(t.name)
      ,
      image(t),
      postForm(cls := "form3", action := routes.RelayTour.update(t.id))(
        inner(form),
        form3.actions(
          a(href := routes.RelayTour.show(t.slug, t.id))(trans.cancel()),
          form3.submit(trans.apply())
        )
      ),
      div(cls := "relay-form__actions")(
        postForm(action := routes.RelayTour.delete(t.id))(
          submitButton(
            cls := "button button-red button-empty confirm"
          )(strong(deleteTournament()), em(definitivelyDeleteTournament()))
        ),
        isGranted(_.Relay) option postForm(action := routes.RelayTour.cloneTour(t.id))(
          submitButton(
            cls := "button button-green button-empty confirm"
          )(strong("Clone as broadcast admin"), em("Clone this broadcast, its rounds, and their studies"))
        )
      )
    )

  private def image(t: RelayTour)(using ctx: PageContext) =
    div(cls := "relay-image-edit", data("post-url") := routes.RelayTour.image(t.id))(
      views.html.relay.tour.thumbnail(t, _.Size.Small)(
        cls               := List("drop-target" -> true, "user-image" -> t.image.isDefined),
        attr("draggable") := "true"
      ),
      div(
        p("Upload a beautiful image to represent your tournament."),
        p("The image must be twice as wide as it is tall. Recommended resolution: 1000x500."),
        p(
          "A picture of the city where the tournament takes place is a good idea, but feel free to design something different."
        ),
        p(trans.streamer.maxSize(s"${lila.memo.PicfitApi.uploadMaxMb}MB.")),
        form3.file.selectImage
      )
    )

  private def layout(title: String, menu: Option[String])(body: Modifier*)(using PageContext) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("relay.form"),
      moreJs = jsModule("relayForm")
    )(menu match
      case Some(active) =>
        main(cls := "page page-menu")(
          tour.pageMenu(active),
          div(cls := "page-menu__content box box-pad")(body)
        )
      case None => main(cls := "page-small box box-pad")(body)
    )

  private def inner(form: Form[Data])(using Context) = frag(
    div(cls := "form-group")(bits.howToUse),
    form3.globalError(form),
    form3.split(
      form3.group(form("name"), tournamentName(), half = true)(form3.input(_)(autofocus)),
      isGranted(_.Relay) option form3.group(
        form("spotlight.title"),
        "Homepage spotlight custom tournament name",
        help = raw("Leave empty to use the tournament name").some,
        half = true
      )(form3.input(_))
    ),
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
        automaticLeaderboard(),
        help = automaticLeaderboardHelp().some,
        half = true
      ),
      form3.group(
        form("players"),
        replace(),
        help = replaceHelp().some,
        half = true
      )(form3.textarea(_)(rows := 3))
    ),
    if isGranted(_.Relay) then
      frag(
        form3.split(
          form3.group(
            form("tier"),
            raw("Official Lichess broadcast tier"),
            help = raw("Feature on /broadcast - for admins only").some,
            half = true
          )(form3.select(_, RelayTour.Tier.options))
        ),
        form3.split(
          form3.checkbox(
            form("spotlight.enabled"),
            "Show a homepage spotlight",
            help = raw("As a Big Blue Button - for admins only").some,
            half = true
          ),
          form3.group(
            form("spotlight.lang"),
            "Homepage spotlight language",
            help = raw("Only show to users who speak this language. English is shown to everyone.").some,
            half = true
          ):
            form3.select(_, lila.i18n.LangForm.popularLanguages.choices)
        )
      )
    else form3.hidden(form("tier"))
  )
