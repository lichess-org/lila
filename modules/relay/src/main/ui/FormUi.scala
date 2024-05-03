package lila.relay
package ui

import scalalib.paginator.Paginator

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import play.api.data.Form
import lila.core.id.ImageId

final class FormUi(helpers: Helpers, ui: RelayUi, tourUi: RelayTourUi):
  import helpers.{ *, given }
  import trans.{ broadcast as trb }

  object round:

    private def page(title: String)(using Context) =
      Page(title)
        .cssTag("relay.form")
        .js(EsmInit("bits.flatpickr"))
        .wrap: body =>
          main(cls := "page-small box box-pad")(body)

    def create(form: Form[RelayRoundForm.Data], tour: RelayTour)(using Context) =
      page(trans.broadcast.newBroadcast.txt()):
        frag(
          boxTop(h1(a(href := routes.RelayTour.edit(tour.id))(tour.name), " • ", trans.broadcast.addRound())),
          standardFlash,
          inner(form, routes.RelayRound.create(tour.id), tour, create = true)
        )

    def edit(rt: RelayRound.WithTour, form: Form[RelayRoundForm.Data])(using Context) =
      page(rt.fullName):
        frag(
          boxTop(
            h1(dataIcon := Icon.Pencil, cls := "text")(
              a(href := routes.RelayTour.edit(rt.tour.id))(rt.tour.name),
              " • ",
              a(href := rt.path)(rt.round.name)
            )
          ),
          inner(form, routes.RelayRound.update(rt.round.id), rt.tour, create = false),
          div(cls := "relay-form__actions")(
            postForm(action := routes.RelayRound.reset(rt.round.id))(
              submitButton(
                cls := "button button-red button-empty confirm"
              )(
                strong(trb.resetRound()),
                em(trb.deleteAllGamesOfThisRound())
              )
            ),
            postForm(action := routes.Study.delete(rt.round.id))(
              submitButton(
                cls := "button button-red button-empty confirm"
              )(strong(trb.deleteRound()), em(trb.definitivelyDeleteRound()))
            )
          )
        )

    private def inner(form: Form[RelayRoundForm.Data], url: play.api.mvc.Call, t: RelayTour, create: Boolean)(
        using ctx: Context
    ) =
      val isLcc = form("syncUrl").value.exists(RelayRound.Sync.UpstreamUrl.LccRegex.matches)
      postForm(cls := "form3", action := url)(
        div(cls := "form-group")(
          ui.howToUse,
          (create && t.createdAt.isBefore(nowInstant.minusMinutes(1))).option:
            p(dataIcon := Icon.InfoCircle, cls := "text"):
              trb.theNewRoundHelp()
        ),
        form3.globalError(form),
        form3.split(
          form3.group(form("name"), trb.roundName(), half = true)(form3.input(_)(autofocus)),
          Granter
            .opt(_.StudyAdmin)
            .option(
              form3.group(
                form("caption"),
                "Homepage spotlight custom round name",
                help = raw("Leave empty to use the round name").some,
                half = true
              ):
                form3.input(_)
            )
        ),
        form3.group(
          form("syncUrl"),
          trb.sourceUrlOrGameIds(),
          help = frag(
            trb.sourceUrlHelp(),
            br,
            trb.gameIdsHelp(),
            br,
            "Or leave empty to push games from another program."
          ).some
        )(form3.input(_)),
        form3
          .group(
            form("syncUrlRound"),
            trb.roundNumber(),
            help = frag("Only for livechesscloud source URLs").some
          )(
            form3.input(_, typ = "number")
          )(cls := (!isLcc).option("none")),
        form3.split(
          form3.group(
            form("startsAt"),
            trb.startDate(),
            help = trb.startDateHelp().some,
            half = true
          )(form3.flatpickr(_, minDate = None)),
          form3.checkbox(
            form("finished"),
            trb.completed(),
            help = trb.completedHelp().some,
            half = true
          )
        ),
        form3.split(
          form3.group(
            form("delay"),
            raw("Delay in seconds"),
            help = frag(
              "Optional, how long to delay moves coming from the source.",
              br,
              "Add this delay to the start date of the event. E.g. if a tournament starts at 20:00 with a delay of 15 minutes, set the start date to 20:15."
            ).some,
            half = true
          )(form3.input(_, typ = "number")),
          Granter
            .opt(_.StudyAdmin)
            .option(
              form3.group(
                form("period"),
                trb.periodInSeconds(),
                help = trb.periodInSecondsHelp().some,
                half = true
              )(form3.input(_, typ = "number"))
            )
        ),
        form3.actions(
          a(href := routes.RelayTour.show(t.slug, t.id))(trans.site.cancel()),
          form3.submit(trans.site.apply())
        )
      )

  object tour:

    private def page(title: String, menu: Option[String])(using Context) =
      Page(title)
        .cssTag("relay.form")
        .js(EsmInit("bits.relayForm"))
        .wrap: body =>
          menu match
            case Some(active) =>
              main(cls := "page page-menu")(
                tourUi.pageMenu(active),
                div(cls := "page-menu__content box box-pad")(body)
              )
            case None => main(cls := "page-small box box-pad")(body)

    def create(form: Form[lila.relay.RelayTourForm.Data])(using Context) =
      page(trans.broadcast.newBroadcast.txt(), menu = "new".some):
        frag(
          boxTop(h1(dataIcon := Icon.RadioTower, cls := "text")(trans.broadcast.newBroadcast())),
          postForm(cls := "form3", action := routes.RelayTour.create)(
            inner(form, none),
            form3.actions(
              a(href := routes.RelayTour.index(1))(trans.site.cancel()),
              form3.submit(trans.site.apply())
            )
          )
        )

    def edit(tg: RelayTour.WithGroupTours, form: Form[RelayTourForm.Data])(using Context) =
      page(tg.tour.name.value, menu = none):
        frag(
          boxTop:
            h1(dataIcon := Icon.Pencil, cls := "text"):
              a(href := routes.RelayTour.show(tg.tour.slug, tg.tour.id))(tg.tour.name)
          ,
          image(tg.tour),
          postForm(cls := "form3", action := routes.RelayTour.update(tg.tour.id))(
            inner(form, tg.some),
            form3.actions(
              a(href := routes.RelayTour.show(tg.tour.slug, tg.tour.id))(trans.site.cancel()),
              form3.submit(trans.site.apply())
            )
          ),
          div(cls := "relay-form__actions")(
            postForm(action := routes.RelayTour.delete(tg.tour.id))(
              submitButton(
                cls := "button button-red button-empty confirm"
              )(strong(trb.deleteTournament()), em(trb.definitivelyDeleteTournament()))
            ),
            Granter
              .opt(_.Relay)
              .option(
                postForm(action := routes.RelayTour.cloneTour(tg.tour.id))(
                  submitButton(
                    cls := "button button-green button-empty confirm"
                  )(
                    strong("Clone as broadcast admin"),
                    em("Clone this broadcast, its rounds, and their studies")
                  )
                )
              )
          )
        )

    private def inner(form: Form[RelayTourForm.Data], tg: Option[RelayTour.WithGroupTours])(using Context) =
      frag(
        div(cls := "form-group")(ui.howToUse),
        form3.globalError(form),
        form3.split(
          form3.group(form("name"), trb.tournamentName(), half = true)(form3.input(_)(autofocus)),
          Granter
            .opt(_.StudyAdmin)
            .option(
              form3.group(
                form("spotlight.title"),
                "Homepage spotlight custom tournament name",
                help = raw("Leave empty to use the tournament name").some,
                half = true
              )(form3.input(_))
            )
        ),
        form3.group(form("description"), trb.tournamentDescription())(form3.textarea(_)(rows := 2)),
        form3.group(
          form("markdown"),
          trb.fullDescription(),
          help = trb
            .fullDescriptionHelp(
              a(
                href := "https://guides.github.com/features/mastering-markdown/",
                targetBlank
              )("Markdown"),
              20000.localize
            )
            .some
        )(form3.textarea(_)(rows := 10)),
        form3.split(
          form3.checkbox(
            form("autoLeaderboard"),
            trb.automaticLeaderboard(),
            help = trb.automaticLeaderboardHelp().some
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
            trb.replacePlayerTags(),
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
        if Granter.opt(_.Relay) then
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
        Granter
          .opt(_.StudyAdmin)
          .option(
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
                  help =
                    raw("Only show to users who speak this language. English is shown to everyone.").some,
                  half = true
                ):
                  form3.select(_, langList.popularLanguagesForm.choices)
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
                  ui.thumbnail(t.tour.pinnedStreamerImage, _.Size.Small16x9)(
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

    private def image(t: RelayTour)(using ctx: Context) =
      div(cls := "relay-image-edit", data("post-url") := routes.RelayTour.image(t.id))(
        ui.thumbnail(t.image, _.Size.Small)(
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

    def grouping(form: Form[RelayTourForm.Data])(using Context) =
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
