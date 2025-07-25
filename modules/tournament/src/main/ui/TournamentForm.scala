package lila.tournament
package ui

import play.api.data.*
import scalalib.paginator.Paginator

import lila.core.i18n.Translate
import lila.core.team.LightTeam
import lila.gathering.GatheringClock
import lila.gathering.ui.GatheringFormUi
import lila.tournament.crud.CrudForm
import lila.ui.*

import ScalatagsTemplate.{ *, given }

opaque type FormPrefix = Option[String]
object FormPrefix extends TotalWrapper[FormPrefix, Option[String]]:
  val empty = FormPrefix(none)
  def make(s: String) = FormPrefix(s.some)

extension (f: Form[?])
  def prefix(name: String)(using prefixOpt: FormPrefix) = f(
    prefixOpt.fold(name)(prefix => s"$prefix.$name")
  )

final class TournamentForm(val helpers: Helpers, showUi: TournamentShow)(
    modMenu: Context ?=> Frag,
    val translatedVariantChoicesWithVariants: Translate ?=> List[(String, String, Option[String])]
):
  import helpers.{ *, given }

  def create(form: Form[?], leaderTeams: List[LightTeam])(using Context) =
    given prefix: FormPrefix = FormPrefix.empty
    val fields = tourFields(form, none)
    Page(trans.site.newTournament.txt())
      .css("tournament.form", "bits.page")
      .js(Esm("bits.tourForm")):
        main(cls := "page-small")(
          div(cls := "tour__form box box-pad")(
            h1(cls := "box__top")(
              if fields.isTeamBattle then trans.arena.newTeamBattle()
              else trans.site.createANewTournament()
            ),
            postForm(cls := "form3", action := routes.Tournament.webCreate)(
              div(cls := "form-group")(
                a(
                  dataIcon := Icon.InfoCircle,
                  cls := "text",
                  href := routes.Cms.lonePage(lila.core.id.CmsPageKey("event-tips"))
                )(trans.site.ourEventTips())
              ),
              setupCreate(form, leaderTeams),
              form3.actions(
                a(href := routes.Tournament.home)(trans.site.cancel()),
                form3.submit(trans.site.createANewTournament(), icon = Icon.Trophy.some)
              )
            )
          ),
          div(cls := "box box-pad tour__faq page")(div(cls := "body")(showUi.faq()))
        )

  def edit(tour: Tournament, form: Form[?], myTeams: List[LightTeam])(using Context) =
    given prefix: FormPrefix = FormPrefix.empty
    Page(tour.name())
      .css("tournament.form")
      .js(Esm("bits.tourForm")):
        main(cls := "page-small")(
          div(cls := "tour__form box box-pad")(
            h1(cls := "box__top")("Edit ", tour.name()),
            postForm(cls := "form3", action := routes.Tournament.update(tour.id))(
              setupEdit(tour, form, myTeams),
              form3.actions(
                a(href := routes.Tournament.show(tour.id))(trans.site.cancel()),
                form3.submit(trans.site.save(), icon = Icon.Trophy.some)
              )
            ),
            hr,
            br,
            br,
            postForm(cls := "terminate", action := routes.Tournament.terminate(tour.id)):
              submitButton(dataIcon := Icon.CautionCircle, cls := "text button button-red yes-no-confirm"):
                trans.site.cancelTournament()
          )
        )

  def tourFields(form: Form[?], tour: Option[Tournament])(using Context, FormPrefix) =
    TourFields(this)(form, tour)

  def setupCreate(form: Form[?], leaderTeams: List[LightTeam])(using Context, FormPrefix) =
    val fields = tourFields(form, none)
    frag(
      form3.globalError(form),
      form3.fieldset("Tournament", toggle = true.some)(
        form3.split(fields.name, fields.minutes),
        form3.split(fields.description)
      ),
      form3.fieldset("Games", toggle = true.some)(
        fields.clock,
        form3.split(fields.variant, fields.startPosition)
      ),
      fields.waitStart,
      conditionFields(form, fields, teams = leaderTeams, tour = none),
      featuresFields(form),
      fields.isTeamBattle.option(form3.hidden(form.prefix("teamBattleByTeam")))
    )

  def setupEdit(tour: Tournament, form: Form[?], myTeams: List[LightTeam])(using Context, FormPrefix) =
    val fields = tourFields(form, tour.some)
    frag(
      form3.globalError(form),
      form3.fieldset("Tournament", toggle = true.some)(
        form3.split(fields.name, fields.minutes),
        form3.split(fields.description)
      ),
      form3.fieldset("Games", toggle = false.some)(
        fields.clock,
        form3.split(fields.variant, fields.startPosition)
      ),
      fields.waitStart,
      conditionFields(form, fields, teams = myTeams, tour = tour.some),
      featuresFields(form)
    )

  private val gatheringFormUi = GatheringFormUi(helpers)

  def conditionFields(
      form: Form[?],
      fields: TourFields,
      teams: List[LightTeam],
      tour: Option[Tournament]
  )(using ctx: Context)(using FormPrefix) =
    val disabledAfterStart = tour.exists(!_.isCreated)
    form3.fieldset("Entry conditions", toggle = tour.exists(_.conditions.list.nonEmpty).some)(
      errMsg(form.prefix("conditions")),
      form3.split(
        fields.entryCode,
        (tour.isEmpty && teams.nonEmpty).option {
          val baseField = form.prefix("conditions.teamMember.teamId")
          val field = ctx.req.queryString
            .get("team")
            .flatMap(_.headOption)
            .foldLeft(baseField): (field, team) =>
              field.copy(value = team.some)
          form3.group(field, trans.site.onlyMembersOfTeam(), half = true):
            form3.select(_, List(("", trans.site.noRestriction.txt())) ::: teams.map(_.pair))
        }
      ),
      form3.split(
        gatheringFormUi.nbRatedGame(form.prefix("conditions.nbRatedGame.nb")),
        gatheringFormUi.accountAge(form.prefix("conditions.accountAge"))
      ),
      form3.split(
        gatheringFormUi.minRating(form.prefix("conditions.minRating.rating")),
        gatheringFormUi.maxRating(form.prefix("conditions.maxRating.rating"))
      ),
      form3.split(
        gatheringFormUi.allowList(form.prefix("conditions.allowList")),
        (ctx.me.exists(_.hasTitle) || Granter.opt(_.ManageTournament)).so {
          gatheringFormUi.titled(form.prefix("conditions.titled"))
        },
        gatheringFormUi.bots(form.prefix("conditions.bots"), disabledAfterStart)
      )
    )

  def featuresFields(form: Form[?])(using ctx: Context)(using FormPrefix) =
    form3.fieldset("Features", toggle = false.some)(
      form3.split(
        form3.checkbox(
          form.prefix("berserkable"),
          trans.arena.allowBerserk(),
          help = trans.arena.allowBerserkHelp().some,
          half = true
        ),
        form3.hiddenFalse(form.prefix("berserkable")),
        form3.checkbox(
          form.prefix("streakable"),
          trans.arena.arenaStreaks(),
          help = trans.arena.arenaStreaksHelp().some,
          half = true
        ),
        form3.hiddenFalse(form.prefix("streakable"))
      ),
      form3.split(
        form3.checkbox(
          form.prefix("rated"),
          trans.site.rated(),
          help = trans.site.ratedFormHelp().some
        ),
        form3.hiddenFalse(form.prefix("rated")),
        form3.checkbox(
          form.prefix("hasChat"),
          trans.site.chatRoom(),
          help = trans.arena.allowChatHelp().some,
          half = true
        ),
        form3.hiddenFalse(form.prefix("hasChat"))
      )
    )

  def spotlightAndTeamBattle(form: Form[?], tour: Option[Tournament])(using Context) =
    frag(
      form3.split(
        form3.group(
          form("homepageHours"),
          raw(s"Hours on homepage (0 to ${CrudForm.maxHomepageHours})"),
          half = true,
          help = raw("Ask on zulip first").some
        )(form3.input(_, typ = "number")),
        form3.group(form("image"), raw("Custom icon"), half = true)(
          form3.select(_, CrudForm.imageChoices)
        )
      ),
      form3.split(
        form3.group(
          form("headline"),
          raw("Homepage headline"),
          help = raw("Keep it VERY short, so it fits on homepage").some,
          half = true
        )(form3.input(_)),
        form3.group(
          form("id"),
          raw("Tournament ID (in the URL)"),
          help =
            raw("An 8-letter unique tournament ID, can't be changed after the tournament is created.").some,
          half = true
        )(f => form3.input(f)(tour.isDefined.option(readonly := true)))
      ),
      form3.checkbox(
        form("teamBattle"),
        raw("Team battle"),
        half = true
      )
    )

  object crud:
    given prefix: FormPrefix = FormPrefix.make("setup")

    private def page(title: String)(using Context) =
      Page(title)
        .js(Esm("bits.tourForm"))
        .wrap: body =>
          main(cls := "page-menu")(modMenu, body)

    def create(form: Form[?])(using Context) =
      page("New tournament").css("mod.form"):
        div(cls := "crud page-menu__content box box-pad")(
          h1(cls := "box__top")("New tournament"),
          postForm(cls := "form3", action := routes.TournamentCrud.create)(
            spotlightAndTeamBattle(form, none),
            errMsg(form("setup")),
            setupCreate(form, Nil),
            form3.action(form3.submit(trans.site.apply()))
          )
        )

    def index(tours: Paginator[Tournament])(using Context) =
      page("Tournament manager")
        .css("mod.misc")
        .js(infiniteScrollEsmInit):
          div(cls := "crud page-menu__content box")(
            boxTop(
              h1("Tournament manager"),
              div(cls := "box__top__actions")(
                a(
                  cls := "button button-green",
                  href := routes.TournamentCrud.form,
                  dataIcon := Icon.PlusButton
                )
              )
            ),
            table(cls := "slist slist-pad")(
              thead(
                tr(
                  th(),
                  th("Variant"),
                  th("Clock"),
                  th("Duration"),
                  th(utcLink, " Date"),
                  th()
                )
              ),
              tbody(cls := "infinite-scroll")(
                tours.currentPageResults.map: tour =>
                  tr(cls := "paginated")(
                    td(
                      a(href := routes.TournamentCrud.edit(tour.id))(
                        strong(tour.name()),
                        " ",
                        em(tour.spotlight.map(_.headline))
                      )
                    ),
                    td(tour.variant.name),
                    td(tour.clock.toString),
                    td(tour.minutes, "m"),
                    td(showInstant(tour.startsAt), " ", momentFromNow(tour.startsAt, alwaysRelative = true)),
                    td(
                      a(
                        href := routes.Tournament.show(tour.id),
                        dataIcon := Icon.Eye,
                        title := "View on site"
                      )
                    )
                  ),
                pagerNextTable(tours, routes.TournamentCrud.index(_).url)
              )
            )
          )

    def edit(tour: Tournament, form: Form[?])(using Context) =
      page(tour.name()).css("mod.form"):
        div(cls := "crud edit page-menu__content box box-pad")(
          boxTop(
            h1(
              a(href := routes.Tournament.show(tour.id))(tour.name()),
              " ",
              span("Created by ", titleNameOrId(tour.createdBy), " on ", showDate(tour.createdAt))
            ),
            st.form(
              cls := "box__top__actions",
              action := routes.TournamentCrud.cloneT(tour.id),
              method := "get"
            )(form3.submit("Clone", Icon.Trophy.some)(cls := "button-green button-empty"))
          ),
          standardFlash,
          postForm(cls := "form3", action := routes.TournamentCrud.update(tour.id))(
            spotlightAndTeamBattle(form, tour.some),
            errMsg(form("setup")),
            setupEdit(tour, form, Nil),
            form3.action(form3.submit(trans.site.apply()))
          )
        )

final class TourFields(tourForm: TournamentForm)(form: Form[?], tour: Option[Tournament])(using
    Context,
    FormPrefix
):
  import tourForm.*
  import tourForm.helpers.{ given, * }

  def isTeamBattle = tour.exists(_.isTeamBattle) || form.prefix("teamBattleByTeam").value.nonEmpty

  private def disabledAfterStart = tour.exists(!_.isCreated)

  def name =
    form3.group(
      form.prefix("name"),
      trans.site.name(),
      half = true,
      help = frag(
        trans.site.safeTournamentName(),
        br,
        trans.site.inappropriateNameWarning(),
        br,
        trans.site.emptyTournamentName()
      ).some
    ) { f =>
      div(
        form3.input(f),
        if isTeamBattle then trans.team.teamBattle() else trans.arena.arena(),
        br
      )
    }(cls := "tour-name")
  def variant =
    form3.group(form.prefix("variant"), trans.site.variant(), half = true)(
      form3.select(
        _,
        translatedVariantChoicesWithVariants.map(x => x._1 -> x._2),
        disabled = disabledAfterStart
      )
    )
  def startPosition =
    form3.group(
      form.prefix("position"),
      trans.site.startPosition(),
      klass = "position",
      half = true,
      help = trans.site
        .positionInputHelp(a(href := "/editor", targetBlank)(trans.site.boardEditor.txt()))
        .some
    ):
      form3.input(_):
        tour.exists(t => !t.isCreated && t.position.isEmpty).option(disabled := true)
  def clock =
    form3.split(
      form3.group(form.prefix("clockTime"), trans.site.clockInitialTime(), half = true):
        form3.select(_, GatheringClock.timeChoices, disabled = disabledAfterStart)
      ,
      form3.group(form.prefix("clockIncrement"), trans.site.clockIncrement(), half = true):
        form3.select(_, GatheringClock.incrementChoices, disabled = disabledAfterStart)
    )
  def minutes =
    form3.group(form.prefix("minutes"), trans.site.duration(), half = true):
      if tour.forall(t => lila.tournament.TournamentForm.minutes contains t.minutes)
      then form3.select(_, TournamentForm.minuteChoicesKeepingCustom(tour))
      else form3.input(_)(tpe := "number")
  def waitMinutes =
    form3.group(form.prefix("waitMinutes"), trans.site.timeBeforeTournamentStarts(), half = true):
      form3.select(_, TournamentForm.waitMinuteChoices)
  def waitStart =
    form3.fieldset("Start date", toggle = tour.forall(_.isCreated).some)(
      form3.split(waitMinutes, startDate)
    )
  def description =
    form3.group(
      form.prefix("description"),
      trans.site.tournDescription(),
      help = trans.site.tournDescriptionHelp().some,
      half = true
    )(form3.textarea(_)(rows := 4))
  def entryCode =
    form3.group(
      form.prefix("password"),
      trans.site.tournamentEntryCode(),
      help = trans.site.makePrivateTournament().some,
      half = true
    )(form3.input(_)(autocomplete := "off"))
  def startDate = tour
    .forall(_.isCreated)
    .option:
      form3.group(
        form.prefix("startDate"),
        trans.arena.customStartDate(),
        help = trans.arena.customStartDateHelp().some,
        half = true
      )(form3.flatpickr(_))
  def advancedSettings =
    frag(
      errMsg(form.prefix("conditions"))
    )
