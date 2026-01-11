package lila.relay
package ui

import play.api.data.{ Field, Form }
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ given, * }
import lila.core.study.Visibility
import chess.tiebreak.Tiebreak

case class FormNavigation(
    group: Option[RelayGroup.WithTours],
    tour: RelayTour,
    rounds: List[RelayRound],
    roundId: Option[RelayRoundId],
    sourceRound: Option[RelayRound.WithTour] = none,
    targetRound: Option[RelayRound.WithTour] = none,
    newRound: Boolean = false
):
  def tourWithGroup = RelayTour.WithGroupTours(tour, group)
  def tourWithRounds = RelayTour.WithRounds(tour, rounds)
  def round = roundId.flatMap(id => rounds.find(_.id == id))
  def featurableRound = round
    .ifTrue(targetRound.isEmpty)
    .filter: r =>
      r.sync.upstream.forall(up => up.isUrl && !up.hasLcc)

final class RelayFormUi(helpers: Helpers, ui: RelayUi, pageMenu: RelayMenuUi):
  import helpers.{ *, given }
  import trans.{ broadcast as trb, site as trs }

  private def navigationMenu(nav: FormNavigation)(using Context) =
    def tourAndRounds(shortName: Option[RelayTour.Name]) = frag(
      a(
        href := routes.RelayTour.edit(nav.tour.id),
        dataIcon := Icon.RadioTower,
        cls := List(
          "text" -> true,
          "relay-form__subnav__tour-parent" -> shortName.isDefined,
          "active" -> (nav.round.isEmpty && !nav.newRound)
        )
      )(
        shortName.fold(frag(nav.tour.name))(strong(_))
      ),
      frag(
        nav.rounds.map: r =>
          a(
            href := routes.RelayRound.edit(r.id),
            cls := List("subnav__subitem text" -> true, "active" -> nav.roundId.has(r.id)),
            dataIcon := (
              if r.isFinished then Icon.Checkmark
              else if r.hasStarted then Icon.DiscBig
              else Icon.DiscOutline
            )
          )(r.transName),
        a(
          href := routes.RelayRound.create(nav.tour.id),
          cls := List(
            "subnav__subitem text" -> true,
            "active" -> nav.newRound,
            "button" -> (nav.rounds.isEmpty && !nav.newRound)
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
    Page("Insufficient permissions").css("bits.relay.form"):
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
        .js(List(Esm("bits.flatpickr"), Esm("bits.relayForm")).map(some))
        .wrap: body =>
          main(cls := "page page-menu")(
            navigationMenu(nav),
            div(cls := "page-menu__content box box-pad")(body)
          )

    def create(form: Form[RelayRoundForm.Data], nav: FormNavigation)(using Context) =
      val newRoundNav = nav.copy(newRound = true)

      page(trb.newBroadcast.txt(), newRoundNav):
        frag(
          boxTop(
            h1(
              a(href := routes.RelayTour.edit(nav.tour.id))(nav.tour.name),
              " / ",
              trb.addRound()
            )
          ),
          standardFlash,
          inner(form, routes.RelayRound.create(nav.tour.id), newRoundNav)
        )

    def edit(
        r: RelayRound,
        form: Form[RelayRoundForm.Data],
        nav: FormNavigation
    )(using Context) =
      page(r.transName, nav):
        val rt = r.withTour(nav.tour)
        frag(
          boxTop(h1(a(href := rt.path)(rt.transName))),
          standardFlash,
          nav.targetRound.map: tr =>
            flashMessage("success")(
              "Your tournament round is officially broadcasted by Lichess!",
              br,
              strong(a(href := tr.path, cls := "text", dataIcon := Icon.RadioTower)(tr.transName)),
              "."
            ),
          inner(form, routes.RelayRound.update(r.id), nav),
          div(cls := "relay-form__actions")(
            postForm(action := routes.RelayRound.reset(r.id))(
              submitButton(
                cls := "button button-red button-empty yes-no-confirm"
              )(
                strong(trb.resetRound()),
                em(trb.deleteAllGamesOfThisRound())
              )
            ),
            postForm(action := routes.Study.delete(r.studyId))(
              submitButton(
                cls := "button button-red button-empty yes-no-confirm"
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
      val lccWarning = for
        round <- nav.round
        upstream <- round.sync.upstream
        if upstream.hasLcc
      yield flashMessage("box relay-form__warning")(
        p(strong("Please use the ", a(href := broadcasterUrl)("Lichess Broadcaster App"))),
        p(
          "LiveChessCloud support is deprecated and will be removed soon.",
          br,
          s"If you need help, please contact us at ",
          broadcastEmailContact,
          "."
        )
      )
      val contactUsForOfficial = nav.featurableRound.isDefined.option:
        flashMessage("box relay-form__contact-us")(
          p(
            "Is this a tournament you organize? Do you want Lichess to feature it on the ",
            a(href := routes.RelayTour.index(1))("broadcast page"),
            "?"
          ),
          p(trans.contact.sendEmailAt(broadcastEmailContact))
        )
      val httpWarning = for
        round <- nav.round
        upstream <- round.sync.upstream
        http <- upstream.hasUnsafeHttp
        https = http.withScheme("https").withPort(-1) // else it adds :80 for some reason
      yield flashMessage("box relay-form__warning")(
        p(
          strong("Warning: a source uses an insecure http:// protocol:"),
          br,
          a(href := http.toString)(http.toString)
        ),
        p("Did you mean ", a(href := https.toString)(https.toString), "?")
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
        form3.group(form("name"), trb.roundName(), help = trb.defaultRoundNameHelp().some)(
          form3.input(_)(autofocus)
        ),
        form3.fieldset("Source", toggle = true.some)(cls := "box-pad")(
          form3.group(
            form("syncSource"),
            "Where do the games come from?"
          )(form3.select(_, RelayRoundForm.sourceTypes)),
          div(cls := "relay-form__sync relay-form__sync-url")(
            httpWarning.orElse(lccWarning).orElse(contactUsForOfficial),
            form3.group(
              form("syncUrl"),
              trb.sourceSingleUrl(),
              help = trb.sourceUrlHelp().some
            )(form3.input(_)),
            nav.sourceRound.map: source =>
              flashMessage("round-push")(
                "Getting real-time updates from ",
                strong(a(href := source.path)(source.transName)),
                br,
                "Owner: ",
                fragList(source.tour.ownerIds.toList.map(u => userIdLink(u.some))),
                br,
                "Delay: ",
                source.round.sync.delay.fold("0")(_.toString),
                "s",
                br,
                "Start: ",
                source.round.startedAt
                  .orElse(source.round.startsAtTime)
                  .fold(frag("unscheduled"))(momentFromNow),
                br,
                "Last sync: ",
                source.round.sync.log.events.lastOption.map: event =>
                  frag(
                    momentFromNow(event.at),
                    br,
                    event.error match
                      case Some(err) => s"❌ $err"
                      case _ => s"✅ ${event.moves} moves"
                  )
              )
          ),
          form3.group(
            form("syncUrls"),
            "Multiple source URLs, one per line.",
            help = frag("The games will be combined in the order of the URLs.").some,
            half = false
          )(field =>
            frag(
              httpWarning.orElse(lccWarning),
              form3.textarea(field)(rows := 5, spellcheck := "false", cls := "monospace")
            )
          )(cls := "relay-form__sync relay-form__sync-urls none"),
          form3.group(
            form("syncIds"),
            trb.sourceGameIds(),
            half = false
          )(form3.input(_))(cls := "relay-form__sync relay-form__sync-ids none"),
          form3.group(
            form("syncUsers"),
            "Up to 100 Lichess usernames, separated by spaces",
            half = false
          )(form3.input(_))(cls := "relay-form__sync relay-form__sync-users none"),
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
            )(form3.input(_)),
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
        form3.fieldset("When does it start", toggle = true.some)(cls := "box-pad")(
          form3.split(
            form3.group(
              form("startsAt"),
              trb.startDateTimeZone(strong(nav.tour.info.timeZoneOrDefault.getId)),
              help = trb.startDateHelp().some,
              half = true
            )(form3.flatpickr(_, local = true, minDate = None)),
            form3.nativeCheckboxField(
              form("startsAfterPrevious"),
              "When the previous round completes",
              help = frag(
                "The start date is unknown, and the round will start automatically when the previous round completes."
              ).some,
              half = true
            )
          )
        ),
        form3.fieldset("Advanced", toggle = nav.round.exists(_.sync.delay.isDefined).some)(
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
            form3.group(
              form("status"),
              "Current status",
              help = frag(
                "Lichess can usually detect the round status, but you can also set it manually if needed."
              ).some,
              half = true
            ):
              form3.select(_, Seq("new" -> "New", "started" -> "Started", "finished" -> "Finished"))
          )
        ),
        form3
          .fieldset("Game ordering", toggle = nav.round.flatMap(_.sync.reorder).isDefined.some)(
            cls := "box-pad"
          )(
            form3.group(
              form("reorder"),
              "Optional: reorder games by player names",
              help = frag( // do not translate
                "One line per game, containing one or two player names.",
                "Example:",
                pre("""Helmut Kleissl
Hanna Marie ; Kozul, Zdenko"""),
                "By default the source game order is used. Extra games are added after the reordered ones."
              ).some,
              half = true
            )(form3.textarea(_)(rows := 7, spellcheck := "false", cls := "monospace"))
          ),
        (nav.tour.showScores || nav.tour.showRatingDiffs).option(
          form3.fieldset(
            "Custom scoring",
            toggle = nav.round.exists(_.customScoring.isDefined).some
          )(
            nav.tour.showRatingDiffs.option(
              form3.group(form("rated"), raw("")): field =>
                val withDefault =
                  if nav.newRound && field.value.isEmpty then field.copy(value = "true".some) else field
                form3.nativeCheckboxField(
                  withDefault,
                  "Rated round",
                  help = frag("Include this round when calculating players' rating changes").some,
                  half = true
                )
            ),
            Color.all.map: color =>
              form3.split:
                List("win", "draw").map: result =>
                  form3.group(
                    form("customScoring")(color.name)(result),
                    raw(s"Points for a $result as ${color.name}")
                  )(
                    form3.input(_)(tpe := "number", step := 0.01f, min := 0.0f, max := 10.0f)
                  )
            ,
            p(
              "Optional. Affects automatic scoring. Points must be >= 0 and <=10. At most 2 decimal places. Default = 1.0 for a win and 0.5 for a draw."
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
                  "Period in seconds",
                  help = frag(
                    "Optional, how long to wait between requests. Min 2s, max 60s. Defaults to automatic based on the number of viewers."
                  ).some,
                  half = true
                )(form3.input(_, typ = "number"))
              )
            )
          ),
        form3.actions(
          a(href := routes.RelayTour.show(nav.tour.slug, nav.tour.id))(trs.cancel()),
          form3.submit(trs.apply())
        )
      )

  object tour:

    private def page(title: String, menu: Either[String, FormNavigation])(using Context) =
      Page(title)
        .css("bits.relay.form")
        .js(Esm("bits.relayForm"))
        .wrap: body =>
          main(cls := "page page-menu")(
            menu.fold(pageMenu(_), navigationMenu),
            div(cls := "page-menu__content box box-pad")(body)
          )

    def create(form: Form[lila.relay.RelayTourForm.Data])(using Context, Me) =
      page(trb.newBroadcast.txt(), menu = Left("new")).markdownTextarea:
        frag(
          boxTop(h1(dataIcon := Icon.RadioTower, cls := "text")(trb.newBroadcast())),
          postForm(cls := "form3", action := routes.RelayTour.create)(
            inner(form, none),
            form3.actions(
              a(href := routes.RelayTour.index(1))(trs.cancel()),
              form3.submit(trs.apply())
            )
          )
        )

    def edit(form: Form[RelayTourForm.Data], nav: FormNavigation)(using Context, Me) =
      page(nav.tour.name.value, menu = Right(nav)).markdownTextarea:
        frag(
          boxTop(h1(a(href := routes.RelayTour.show(nav.tour.slug, nav.tour.id))(nav.tour.name))),
          standardFlash,
          image(nav.tour),
          postForm(cls := "form3", action := routes.RelayTour.update(nav.tour.id))(
            inner(form, nav.tourWithGroup.some),
            form3.actions(
              a(href := routes.RelayTour.show(nav.tour.slug, nav.tour.id))(trs.cancel()),
              form3.submit(trs.apply())
            )
          ),
          div(cls := "relay-form__actions")(
            postForm(action := routes.RelayTour.delete(nav.tour.id))(
              submitButton(
                cls := "button button-red button-empty yes-no-confirm"
              )(strong(trb.deleteTournament()), em(trb.definitivelyDeleteTournament()))
            ),
            Granter
              .opt(_.Relay)
              .option(
                postForm(action := routes.RelayTour.cloneTour(nav.tour.id))(
                  submitButton(
                    cls := "button button-green button-empty yes-no-confirm"
                  )(
                    strong("Clone as broadcast admin"),
                    em("Clone this broadcast, its rounds, and their studies")
                  )
                )
              )
          )
        )

    private val sortedTiebreaks = Tiebreak.preset.sortBy(_.extendedCode)

    private def inner(form: Form[RelayTourForm.Data], tg: Option[RelayTour.WithGroupTours])(using
        Context,
        Me
    ) =
      frag(
        (!Granter.opt(_.StudyAdmin)).option(div(cls := "form-group")(ui.howToUse)),
        form3.globalError(form),
        form3.group(form("name"), trb.tournamentName())(form3.input(_)(autofocus)),
        form3.fieldset(trb.optionalDetails(), toggle = tg.exists(_.tour.info.nonEmpty).some)(
          form3.split(
            form3.group(
              form("info.format"),
              trb.tournamentFormat(),
              help = frag("""e.g. "8-player round-robin" or "5-round Swiss"""").some,
              half = true
            )(form3.input(_)),
            form3.group(
              form("info.location"),
              trb.tournamentLocation(),
              half = true
            )(form3.input(_))
          ),
          form3.split(
            form3.group(
              form("info.players"),
              trb.topPlayers(),
              help = frag("Mention up to 4 of the best players participating").some,
              half = true
            )(form3.input(_)),
            form3.group(
              form("info.timeZone"),
              trb.timezone(),
              help = frag("Used to set round dates using their local time").some,
              half = true
            ):
              form3.select(_, timeZone.translatedChoices)
          ),
          form3.split(
            form3.group(
              form("info.tc"),
              trs.timeControl(),
              help = frag("""e.g. "15 min + 10 sec" or "15+10"""").some,
              half = true
            )(form3.input(_)),
            form3.group(
              form("info.fideTc"),
              trb.fideRatingCategory(),
              help = frag("Which FIDE ratings to use").some,
              half = true
            ):
              form3.select(
                _,
                chess.FideTC.values.map: tc =>
                  tc.toString -> tc.toString.capitalize
              )
          ),
          form3.split(
            form3.group(
              form("info.website"),
              trb.officialWebsite(),
              help = frag("External website URL").some,
              half = true
            )(form3.input(_)),
            form3.group(
              form("info.standings"),
              trb.officialStandings(),
              help = frag("External website URL, e.g. chess-results.com, info64.org").some,
              half = true
            )(form3.input(_))
          ),
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
          ): field =>
            lila.ui.bits.markdownTextarea("broadcastDescription".some):
              form3.textarea(field)(rows := 10)
        ),
        form3
          .fieldset(
            "Features",
            toggle = tg
              .map(_.tour)
              .exists: t =>
                !t.showScores || !t.showRatingDiffs || t.teamTable || !t.isPublic
              .some
          )(
            form3.split(
              form3.nativeCheckboxField(
                form("showScores"),
                trb.showScores(),
                help = None,
                half = true
              ),
              form3.nativeCheckboxField(
                form("showRatingDiffs"),
                "Show player's rating diffs",
                help = None,
                half = true
              )
            ),
            form3.split(
              form3.nativeCheckboxField(
                form("teamTable"),
                trans.team.teamTournament(),
                help = frag("Show a team leaderboard. Requires WhiteTeam and BlackTeam PGN tags.").some,
                half = true
              ),
              form3.group(
                form("visibility"),
                trans.study.visibility(),
                half = true
              )(
                form3.select(
                  _,
                  List(
                    Visibility.public.key -> "Public",
                    Visibility.unlisted.key -> "Unlisted (from URL only)",
                    Visibility.`private`.key -> "Private (invited members only)"
                  )
                )
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
                pre("player name / FIDE ID"),
                "Example:",
                pre("""Magnus Carlsen / 1503014"""),
                "Player names ignore case and punctuation, and match all possible combinations of 2 words:",
                br,
                """"Jorge Rick Vito" will match "Jorge Rick", "jorge vito", "Rick, Vito", etc.""",
                br,
                "If the player is NM or WNM, you can:",
                pre("""Player Name / FIDE ID / title"""),
                "Alternatively, you may set tags manually, like so:",
                pre("player name / FIDE ID / title / rating / new name"),
                "All values are optional. Example:",
                pre("""Magnus Carlsen / / GM / 2863
YouGotLittUp / / / 1890 / Louis Litt""")
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
        form3.fieldset("Tiebreaks", toggle = tg.map(_.tour).exists(_.tiebreaks.isDefined).some):
          form3.split(
            (0 until 5).map: i =>
              form3.group(form(s"tiebreaks[$i]"), s"Tiebreak ${i + 1}", half = true):
                form3.select(
                  _,
                  sortedTiebreaks.map: t =>
                    t.extendedCode -> s"${t.description} (${t.extendedCode})",
                  default = "Optional. Select a tiebreak".some
                )
            ,
            p(dataIcon := Icon.InfoCircle, cls := "text")(
              "Tiebreaks are best suited for round-robin tournaments where all games are broadcasted and played. ",
              "Tiebreaks will differ from official results if the tiebreak method utilises byes and forfeits."
            )
          )
        ,
        tg.isDefined.option:
          form3.fieldset("Grouping", toggle = false.some):
            grouping(form)
        ,
        if Granter.opt(_.Relay) then
          frag(
            form3.fieldset("Broadcast admin", toggle = true.some)(
              form3.split(
                form3.group(
                  form("tier"),
                  raw("Official Lichess broadcast tier"),
                  help = raw("Priority and ranking - for admins only").some,
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
                    form3.split(
                      form3.checkbox(
                        form("orphanWarn"),
                        "Warn about Orphan Boards",
                        help = raw(
                          "Send a warning to the Broadcast team when boards are not receiving updates from the source. Disable if there are manually entered boards."
                        ).some,
                        half = true
                      )
                    )
                  )
                )
            ),
            (tg.isDefined && Granter.opt(_.StudyAdmin)).option:
              form3.fieldset("Pinned stream", toggle = form("pinnedStream.url").value.isDefined.some)(
                form3.split(
                  form3.group(
                    form("pinnedStream.url"),
                    "Stream URL",
                    help = frag(
                      p("Embed a live stream in the broadcast. Examples:"),
                      ul(
                        li("https://www.youtube.com/live/Lg0askmGqvo"),
                        li("https://www.twitch.tv/tcec_chess_tv")
                      )
                    ).some,
                    half = true
                  )(form3.input(_)),
                  form3.group(
                    form("pinnedStream.name"),
                    "Stream name",
                    half = true
                  )(form3.input(_))
                ),
                form3.split(
                  form3.group(
                    form("pinnedStream.text"),
                    "Stream link label",
                    help = frag(
                      "Optional. Show a label on the image link to your live stream.",
                      br,
                      "Example: 'Watch us live on YouTube!'"
                    ).some
                  )(form3.input(_))
                )
              )
          )
        else form3.hidden(form("tier")),
        form3.fieldset("Broadcaster note", toggle = tg.flatMap(_.tour.note).isDefined.some)(
          form3.group(
            form("note"),
            "Note for contributors. This is not shown to viewers."
          )(form3.textarea(_)(rows := 4))
        )
      )

  private def image(t: RelayTour)(using ctx: Context) =
    form3.fieldset("Image", toggle = true.some):
      div(
        cls := "form-group relay-image-edit",
        data("post-url") := routes.RelayTour.image(t.id)
      )(
        ui.thumbnail(t.image, _.Size.Small)(
          cls := List("drop-target" -> true, "user-image" -> t.image.isDefined),
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
    div(cls := "relay-form__grouping")(
      form3.group(
        form("grouping.info"),
        "Optional: assign tournaments to a group",
        help = frag( // do not translate
          "First line is the group name.",
          br,
          "Subsequent lines are URLs of tournaments that will be part of the group.",
          br,
          "You can add, remove, and re-order tournaments; and you can rename the group.",
          br,
          "Example:",
          pre("""Dutch Championships 2025
https://lichess.org/broadcast/dutch-championships-2025--open--first-stage/ISdmqct3
https://lichess.org/broadcast/dutch-championships-2025--women--first-stage/PGFBkEha
https://lichess.org/broadcast/dutch-championships-2025--open--quarterfinals/Zi12QchK
""")
        ).some
      )(form3.textarea(_)(rows := 5, spellcheck := "false", cls := "monospace")),
      form3.group(
        form("grouping.scoreGroups"),
        "Optional: Divide the group into score groups",
        help = frag(
          "Each line defines a new score group with comma-separated tournament IDs.",
          br,
          "Only tournaments that are part of this group can be used in score groups.",
          br,
          "Settings for scores, rating diffs and tiebreaks are taken from the first tournament in each score group.",
          br,
          "Example:",
          pre("""ISdmqct3,Zi12QchK
PGFBkEha"""),
          "Using the same example as above, this will create 2 score groups:",
          br,
          "1) Combines the open sections",
          br,
          "2) Is the lone women's section"
        ).some
      )(form3.textarea(_)(rows := 3, spellcheck := "false", cls := "monospace"))
    )
