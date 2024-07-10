package lila.relay
package ui

import scalalib.paginator.Paginator

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import play.api.data.Form

case class FormNavigation(
    group: Option[RelayGroup.WithTours],
    tour: RelayTour,
    rounds: List[RelayRound],
    roundId: Option[RelayRoundId],
    sourceRound: Option[RelayRound.WithTour] = none,
    targetRound: Option[RelayRound.WithTour] = none,
    newRound: Boolean = false
):
  def tourWithGroup  = RelayTour.WithGroupTours(tour, group)
  def tourWithRounds = RelayTour.WithRounds(tour, rounds)
  def round          = roundId.flatMap(id => rounds.find(_.id == id))
  def featurableRound = round
    .ifTrue(targetRound.isEmpty)
    .filter: r =>
      r.sync.upstream.forall(up => up.isUrl && !up.hasLcc)

final class FormUi(helpers: Helpers, ui: RelayUi, tourUi: RelayTourUi):
  import helpers.{ *, given }
  import trans.{ broadcast as trb }

  private def navigationMenu(nav: FormNavigation)(using Context) =
    def tourAndRounds(shortName: Option[RelayTour.Name]) = frag(
      a(
        href     := routes.RelayTour.edit(nav.tour.id),
        dataIcon := Icon.RadioTower,
        cls := List(
          "text"                            -> true,
          "relay-form__subnav__tour-parent" -> shortName.isDefined,
          "active"                          -> (nav.round.isEmpty && !nav.newRound)
        )
      )(
        shortName.fold(frag(nav.tour.name))(strong(_))
      ),
      frag(
        nav.rounds.map: r =>
          a(
            href := routes.RelayRound.edit(r.id),
            cls  := List("subnav__subitem text" -> true, "active" -> nav.roundId.has(r.id)),
            dataIcon := (
              if r.finished then Icon.Checkmark
              else if r.hasStarted then Icon.DiscBig
              else Icon.DiscOutline
            )
          )(r.name),
        a(
          href := routes.RelayRound.create(nav.tour.id),
          cls := List(
            "subnav__subitem text" -> true,
            "active"               -> nav.newRound,
            "button"               -> (nav.rounds.isEmpty && !nav.newRound)
          ),
          dataIcon := Icon.PlusButton
        )(trb.addRound())
      )
    )
    lila.ui.bits.pageMenuSubnav(
      cls := "relay-form__subnav",
      nav.group match
        case None => tourAndRounds(none)
        case Some(g) =>
          frag(
            span(cls := "relay-form__subnav__group")(g.group.name),
            g.withShorterTourNames.tours.map: t =>
              if nav.tour.id == t.id then tourAndRounds(t.name.some)
              else a(href := routes.RelayTour.edit(t.id), cls := List("subnav__item" -> true))(t.name)
          )
    )

  def noAccess(nav: FormNavigation)(using Context) =
    Page("Insufficient permissions")
      .css("bits.relay.form")
      .wrap: body =>
        main(cls := "page page-menu")(
          navigationMenu(nav),
          div(cls := "page-menu__content box box-pad")(
            boxTop(h1("Insufficient permissions")),
            p("You are not allowed to edit this broadcast or round.")
          )
        )

  object round:

    private def page(title: String, nav: FormNavigation)(using Context) =
      Page(title)
        .css("bits.relay.form")
        .js(List(EsmInit("bits.flatpickr"), EsmInit("bits.relayForm")).map(some))
        .wrap: body =>
          main(cls := "page page-menu")(
            navigationMenu(nav),
            div(cls := "page-menu__content box box-pad")(body)
          )

    def create(form: Form[RelayRoundForm.Data], nav: FormNavigation)(using Context) =
      page(trans.broadcast.newBroadcast.txt(), nav.copy(newRound = true)):
        frag(
          boxTop(
            h1(
              a(href := routes.RelayTour.edit(nav.tour.id))(nav.tour.name),
              " / ",
              trans.broadcast.addRound()
            )
          ),
          standardFlash,
          inner(form, routes.RelayRound.create(nav.tour.id), nav)
        )

    def edit(
        r: RelayRound,
        form: Form[RelayRoundForm.Data],
        nav: FormNavigation
    )(using Context) =
      page(r.name.value, nav):
        val rt = r.withTour(nav.tour)
        frag(
          boxTop(h1(a(href := rt.path)(rt.fullName))),
          standardFlash,
          nav.targetRound.map: tr =>
            flashMessage("success")(
              "Your tournament round is officially broadcasted by Lichess!",
              br,
              strong(a(href := tr.path, cls := "text", dataIcon := Icon.RadioTower)(tr.fullName)),
              "."
            ),
          inner(form, routes.RelayRound.update(r.id), nav),
          div(cls := "relay-form__actions")(
            postForm(action := routes.RelayRound.reset(r.id))(
              submitButton(
                cls := "button button-red button-empty confirm"
              )(
                strong(trb.resetRound()),
                em(trb.deleteAllGamesOfThisRound())
              )
            ),
            postForm(action := routes.Study.delete(r.studyId))(
              submitButton(
                cls := "button button-red button-empty confirm"
              )(strong(trb.deleteRound()), em(trb.definitivelyDeleteRound()))
            )
          )
        )

    private def inner(
        form: Form[RelayRoundForm.Data],
        url: play.api.mvc.Call,
        nav: FormNavigation
    )(using ctx: Context) =
      val broadcastEmailContact = a(href := "mailto:broadcast@lichess.org")("broadcast@lichess.org")
      val lccWarning = nav.round
        .flatMap(_.sync.upstream)
        .exists(_.hasLcc)
        .option:
          flashMessage("box relay-form__lcc-deprecated")(
            p(strong("Please use the ", a(href := broadcasterUrl)("Lichess Broadcaster App"))),
            p(
              "LiveChessCloud support is deprecated and will be removed soon.",
              br,
              s"If you need help, please contact us at ",
              broadcastEmailContact,
              "."
            )
          )
      val contactUsForOfficial = nav.featurableRound.isDefined
        .option:
          flashMessage("box relay-form__contact-us")(
            p(
              "Is this a tournament you organize? Do you want Lichess to feature it on the ",
              a(href := routes.RelayTour.index(1))("broadcast page"),
              "?"
            ),
            p(trans.contact.sendEmailAt(broadcastEmailContact))
          )
      postForm(cls := "form3", action := url)(
        (!Granter.opt(_.StudyAdmin)).option:
          div(cls := "form-group")(
            div(cls := "form-group")(ui.howToUse),
            (nav.round.isEmpty && nav.tour.createdAt.isBefore(nowInstant.minusMinutes(1))).option:
              p(dataIcon := Icon.InfoCircle, cls := "text"):
                trb.theNewRoundHelp()
          )
        ,
        form3.globalError(form),
        form3.split(
          form3.group(form("name"), trb.roundName(), half = true)(form3.input(_)(autofocus)),
          form3.group(
            form("startsAt"),
            trb.startDate(),
            help = trb.startDateHelp().some,
            half = true
          )(form3.flatpickr(_, minDate = None))
        ),
        form3.fieldset("Source", toggle = true.some)(cls := "box-pad")(
          form3.group(
            form("syncSource"),
            "Where do the games come from?"
          )(form3.select(_, RelayRoundForm.sourceTypes)),
          div(cls := "relay-form__sync relay-form__sync-url")(
            lccWarning.orElse(contactUsForOfficial),
            form3.group(
              form("syncUrl"),
              trb.sourceSingleUrl(),
              help = trb.sourceUrlHelp().some
            )(form3.input(_)),
            nav.sourceRound.map: source =>
              flashMessage("round-push")(
                "Getting real-time updates from ",
                strong(a(href := source.path)(source.fullName)),
                br,
                "Owner: ",
                userIdLink(source.tour.ownerId.some),
                br,
                "Delay: ",
                source.round.sync.delay.fold("0")(_.toString),
                "s",
                br,
                "Start: ",
                source.round.startedAt.orElse(source.round.startsAt).fold(frag("unscheduled"))(momentFromNow),
                br,
                "Last sync: ",
                source.round.sync.log.events.lastOption.map: event =>
                  frag(
                    momentFromNow(event.at),
                    br,
                    event.error match
                      case Some(err) => s"❌ $err"
                      case _         => s"✅ ${event.moves} moves"
                  )
              )
          ),
          form3.group(
            form("syncUrls"),
            "Multiple source URLs, one per line.",
            help = frag("The games will be combined in the order of the URLs.").some,
            half = false
          )(field =>
            frag(lccWarning, form3.textarea(field)(rows := 5, spellcheck := "false", cls := "monospace"))
          )(cls := "relay-form__sync relay-form__sync-urls none"),
          form3.group(
            form("syncIds"),
            trb.sourceGameIds(),
            half = false
          )(form3.input(_))(cls := "relay-form__sync relay-form__sync-ids none"),
          div(cls := "form-group relay-form__sync relay-form__sync-push none")(
            contactUsForOfficial,
            p(
              "Send your local games to Lichess using the ",
              a(href := broadcasterUrl)("Lichess Broadcaster App"),
              "."
            )
          ),
          form3.split(cls := "relay-form__sync relay-form__sync-url relay-form__sync-urls none")(
            form3.group(
              form("onlyRound"),
              raw("Filter games by round number"),
              help = frag(
                "Optional, only keep games from the source that match a round number.",
                br,
                "It uses the PGN ",
                strong("Round"),
                " tag. These would match round 3:",
                pre(
                  """[Round "3"]
[Round "3.1"]"""
                ),
                "If you set a round number, then games without a ",
                strong("Round"),
                " tag are dropped."
              ).some,
              half = true
            )(form3.input(_, typ = "number")),
            form3.group(
              form("slices"),
              raw("Select slices of the games"),
              help = frag(
                "Optional. Select games based on their position in the source.",
                br,
                pre("""
1           only select the first board
1-4         only select the first 4 boards
1,2,3,4     same as above, first 4 boards
11-15,21-25 boards 11 to 15, and boards 21 to 25
2,3,7-9     boards 2, 3, 7, 8, and 9
"""),
                "Slicing is done after filtering by round number."
              ).some,
              half = true
            )(form3.input(_))
          )
        ),
        form3.fieldset("Advanced", toggle = nav.round.exists(r => r.sync.delay.isDefined).some)(
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
            form3.checkbox(
              form("finished"),
              trb.completed(),
              help = trb.completedHelp().some,
              half = true
            )
          )
        ),
        Granter
          .opt(_.StudyAdmin)
          .option(
            form3.fieldset("Broadcast admin", toggle = false.some)(
              form3.split(
                form3.group(
                  form("caption"),
                  "Homepage spotlight custom round name",
                  help = raw("Leave empty to use the round name").some,
                  half = true
                ):
                  form3.input(_)
                ,
                form3.group(
                  form("period"),
                  trb.periodInSeconds(),
                  help = trb.periodInSecondsHelp().some,
                  half = true
                )(form3.input(_, typ = "number"))
              )
            )
          ),
        form3.actions(
          a(href := routes.RelayTour.show(nav.tour.slug, nav.tour.id))(trans.site.cancel()),
          form3.submit(trans.site.apply())
        )
      )

  object tour:

    private def page(title: String, menu: Either[String, FormNavigation])(using Context) =
      Page(title)
        .css("bits.relay.form")
        .js(EsmInit("bits.relayForm"))
        .wrap: body =>
          main(cls := "page page-menu")(
            menu.fold(tourUi.pageMenu(_), navigationMenu),
            div(cls := "page-menu__content box box-pad")(body)
          )

    def create(form: Form[lila.relay.RelayTourForm.Data])(using Context) =
      page(trans.broadcast.newBroadcast.txt(), menu = Left("new")):
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

    def edit(form: Form[RelayTourForm.Data], nav: FormNavigation)(using Context) =
      page(nav.tour.name.value, menu = Right(nav)):
        frag(
          boxTop(h1(a(href := routes.RelayTour.show(nav.tour.slug, nav.tour.id))(nav.tour.name))),
          standardFlash,
          image(nav.tour),
          postForm(cls := "form3", action := routes.RelayTour.update(nav.tour.id))(
            inner(form, nav.tourWithGroup.some),
            form3.actions(
              a(href := routes.RelayTour.show(nav.tour.slug, nav.tour.id))(trans.site.cancel()),
              form3.submit(trans.site.apply())
            )
          ),
          div(cls := "relay-form__actions")(
            postForm(action := routes.RelayTour.delete(nav.tour.id))(
              submitButton(
                cls := "button button-red button-empty confirm"
              )(strong(trb.deleteTournament()), em(trb.definitivelyDeleteTournament()))
            ),
            Granter
              .opt(_.Relay)
              .option(
                postForm(action := routes.RelayTour.cloneTour(nav.tour.id))(
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
        (!Granter.opt(_.StudyAdmin)).option(div(cls := "form-group")(ui.howToUse)),
        form3.globalError(form),
        form3.group(form("name"), trb.tournamentName())(form3.input(_)(autofocus)),
        form3.fieldset("Optional details", toggle = tg.exists(_.tour.info.nonEmpty).some)(
          form3.split(
            form3.group(
              form("info.format"),
              "Tournament format",
              help = frag("""e.g. "8-player round-robin" or "5-round Swiss"""").some,
              half = true
            )(form3.input(_)),
            form3.group(
              form("info.tc"),
              "Time control",
              help = frag(""""Classical" or "Rapid" or "Rapid & Blitz"""").some,
              half = true
            )(form3.input(_))
          ),
          form3.group(
            form("info.players"),
            "Top players",
            help = frag("Mention up to 4 of the best players participating").some
          )(form3.input(_)),
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
          )(form3.textarea(_)(rows := 10))
        ),
        form3
          .fieldset("Features", toggle = tg.map(_.tour).exists(t => t.autoLeaderboard || t.teamTable).some)(
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
            )
          ),
        form3.fieldset(
          "Players & Teams",
          toggle = List("players", "teams").exists(k => form(k).value.exists(_.nonEmpty)).some
        )(
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
                "If the player is NM or WNM, you can:",
                pre("""Player Name = FIDE ID / Title"""),
                "Alternatively, you may set tags manually, like so:",
                pre("player name / rating / title / new name"),
                "All values are optional. Example:",
                pre("""Magnus Carlsen / 2863 / GM
YouGotLittUp / 1890 / / Louis Litt""")
              ).some,
              half = true
            )(form3.textarea(_)(rows := 3, spellcheck := "false", cls := "monospace")),
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
            )(form3.textarea(_)(rows := 3, spellcheck := "false", cls := "monospace"))
          )
        ),
        if Granter.opt(_.Relay) then
          frag(
            form3.fieldset("Broadcast admin", toggle = true.some)(
              tg.isDefined.option(grouping(form)),
              form3.split(
                form3.group(
                  form("tier"),
                  raw("Official Lichess broadcast tier"),
                  help = raw("Feature on /broadcast - for admins only").some,
                  half = true
                )(form3.select(_, RelayTour.Tier.options)),
                Granter
                  .opt(_.StudyAdmin)
                  .option(
                    form3.checkbox(
                      form("spotlight.enabled"),
                      "Show a homepage spotlight",
                      help = raw("As a Big Blue Button - for admins only").some,
                      half = true
                    )
                  )
              ),
              Granter
                .opt(_.StudyAdmin)
                .option(
                  frag(
                    form3.split(
                      form3.group(
                        form("spotlight.title"),
                        "Homepage spotlight custom tournament name",
                        help = raw("Leave empty to use the tournament name").some,
                        half = true
                      )(form3.input(_)),
                      form3.group(
                        form("spotlight.lang"),
                        "Homepage spotlight language",
                        help = raw(
                          "Only show to users who speak this language. English is shown to everyone."
                        ).some,
                        half = true
                      ):
                        form3.select(_, langList.popularLanguagesForm.choices)
                    ),
                    tg.map: t =>
                      form3.fieldset("Pinned streamer", toggle = form("pinnedStreamer").value.isDefined.some)(
                        div(
                          cls              := "relay-pinned-streamer-edit",
                          data("post-url") := routes.RelayTour.image(t.tour.id, "pinnedStreamerImage".some)
                        )(
                          div(
                            form3.group(
                              form("pinnedStreamer"),
                              "Pinned streamer",
                              help = frag(
                                p(
                                  "The pinned streamer is featured even when they're not watching the broadcast."
                                ),
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
                                tpe := "button",
                                cls := "button button-empty button-red streamer-delete-image",
                                data("post-url") := routes.RelayTour
                                  .image(t.tour.id, "pinnedStreamerImage".some)
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
            )
          )
        else form3.hidden(form("tier"))
      )

  private def image(t: RelayTour)(using ctx: Context) =
    form3.fieldset("Image", toggle = true.some):
      div(
        cls              := "form-group relay-image-edit",
        data("post-url") := routes.RelayTour.image(t.id)
      )(
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

  private def grouping(form: Form[RelayTourForm.Data])(using Context) =
    form3.split(cls := "relay-form__grouping")(
      form3.group(
        form("grouping"),
        "Optional: assign tournaments to a group",
        half = true
      )(form3.textarea(_)(rows := 5, spellcheck := "false", cls := "monospace")),
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
