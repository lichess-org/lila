package views.html.relay

import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.relay.RelayTour
import lila.relay.RelayTourForm.Data

object tourForm:

  import trans.broadcast.*

  def create(form: Form[Data])(using PageContext) =
    layout(newBroadcast.txt(), menu = "new".some)(
      boxTop(h1(dataIcon := licon.RadioTower, cls := "text")(newBroadcast())),
      postForm(cls := "form3", action := routes.RelayTour.create)(
        inner(form, none),
        form3.actions(
          a(href := routes.RelayTour.index(1))(trans.site.cancel()),
          form3.submit(trans.site.apply())
        )
      )
    )

  def edit(tg: RelayTour.WithGroupTours, form: Form[Data])(using PageContext) =
    import tg.*
    layout(tour.name.value, menu = none)(
      boxTop:
        h1(dataIcon := licon.Pencil, cls := "text"):
          a(href := routes.RelayTour.show(tour.slug, tour.id))(tour.name)
      ,
      image(tour),
      postForm(cls := "form3", action := routes.RelayTour.update(tour.id))(
        inner(form, tg.some),
        form3.actions(
          a(href := routes.RelayTour.show(tour.slug, tour.id))(trans.site.cancel()),
          form3.submit(trans.site.apply())
        )
      ),
      div(cls := "relay-form__actions")(
        postForm(action := routes.RelayTour.delete(tour.id))(
          submitButton(
            cls := "button button-red button-empty confirm"
          )(strong(deleteTournament()), em(definitivelyDeleteTournament()))
        ),
        isGranted(_.Relay).option(
          postForm(action := routes.RelayTour.cloneTour(tour.id))(
            submitButton(
              cls := "button button-green button-empty confirm"
            )(strong("Clone as broadcast admin"), em("Clone this broadcast, its rounds, and their studies"))
          )
        )
      )
    )

  private def image(t: RelayTour)(using ctx: PageContext) =
    div(cls := "relay-image-edit", data("post-url") := routes.RelayTour.image(t.id))(
      views.html.relay.tour.thumbnail(t.image, _.Size.Small)(
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
        form3.file.selectImage()
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

  private def inner(form: Form[Data], tg: Option[RelayTour.WithGroupTours])(using Context) = frag(
    div(cls := "form-group")(bits.howToUse),
    form3.globalError(form),
    form3.split(
      form3.group(form("name"), tournamentName(), half = true)(form3.input(_)(autofocus)),
      isGranted(_.StudyAdmin).option(
        form3.group(
          form("spotlight.title"),
          "Homepage spotlight custom tournament name",
          help = raw("Leave empty to use the tournament name").some,
          half = true
        )(form3.input(_))
      )
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
        help = automaticLeaderboardHelp().some
      ),
      form3.checkbox(
        form("teamTable"),
        "Team tournament",
        help = frag("Show a team leaderboard. Requires WhiteTeam and BlackTeam PGN tags.").some
      )
    ),
    form3.split(
      form3.group(
        form("players"),
        replacePlayerTags(),
        help = frag( // do not translate
          "One line per player, formatted as such:",
          pre("player name = FIDE ID"),
          "Example:",
          pre("""Magnus Carlsen = 1503014"""),
          "Player names ignore case and punctuation, and match all possible combinations of 2 words:",
          br,
          """"Jorge Rick Vito" will match "Jorge Rick", "jorge vito", "Rick, Vito", etc.""",
          br,
          "Alternatively, you may set tags manually, like so:",
          pre("player name / rating / title / new name"),
          "All values are optional. Example:",
          pre("""Magnus Carlsen / 2863 / GM
YouGotLittUp / 1890 / / Louis Litt""")
        ).some,
        half = true
      )(form3.textarea(_)(rows := 3)),
      form3.group(
        form("teams"),
        "Optional: assign players to teams",
        help = frag( // do not translate
          "One line per player, formatted as such:",
          pre("Team name; Fide Id or Player name"),
          "Example:",
          pre("""Team Cats ; 3408230
Team Dogs ; Scooby Doo"""),
          "By default the PGN tags WhiteTeam and BlackTeam are used."
        ).some,
        half = true
      )(form3.textarea(_)(rows := 3))
    ),
    if isGranted(_.Relay) then
      frag(
        tg.isDefined.option(grouping(form)),
        form3.split(
          form3.group(
            form("tier"),
            raw("Official Lichess broadcast tier"),
            help = raw("Feature on /broadcast - for admins only").some,
            half = true
          )(form3.select(_, RelayTour.Tier.options))
        )
      )
    else form3.hidden(form("tier")),
    isGranted(_.StudyAdmin).option(
      frag(
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
        ),
        tg.map: t =>
          div(
            cls              := "relay-pinned-streamer-edit",
            data("post-url") := routes.RelayTour.image(t.tour.id, "pinnedStreamerImage".some)
          )(
            div(
              form3.group(
                form("pinnedStreamer"),
                "Pinned streamer",
                help = frag(
                  p("The pinned streamer is featured even when they're not watching the broadcast."),
                  p("An optional placeholder image will embed their stream when clicked."),
                  p(
                    "To upload one, you must first submit this form with a pinned streamer. "
                      + "Then return to this page and choose an image."
                  )
                ).some
              )(form3.input(_)),
              span(
                button(tpe := "button", cls := "button streamer-select-image")("select image"),
                button(
                  tpe              := "button",
                  cls              := "button button-empty button-red streamer-delete-image",
                  data("post-url") := routes.RelayTour.image(t.tour.id, "pinnedStreamerImage".some)
                )("delete image")
              )
            ),
            views.html.relay.tour.thumbnail(t.tour.pinnedStreamerImage, _.Size.Small16x9)(
              cls := List(
                "streamer-drop-target" -> true,
                "user-image"           -> t.tour.pinnedStreamerImage.isDefined
              ),
              attr("draggable") := "true"
            )
          )
      )
    )
  )

  def grouping(form: Form[Data])(using Context) =
    form3.split(cls := "relay-form__grouping")(
      form3.group(
        form("grouping"),
        "Optional: assign tournaments to a group",
        half = true
      )(form3.textarea(_)(rows := 5)),
      div(cls := "form-group form-half form-help")( // do not translate
        "First line is the group name. Subsequent lines are the tournament IDs and names in the group. Names are facultative and only used for display in this textarea.",
        br,
        "You can add, remove, and re-order tournaments; and you can rename the group.",
        br,
        "Example:",
        pre("""Youth Championship 2024
tour1-id Youth Championship 2024 | G20
tour2-id Youth Championship 2024 | G16
""")
      )
    )
