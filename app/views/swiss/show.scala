package views.swiss

import play.api.libs.json.Json

import lila.app.templating.Environment.{ *, given }

import scalalib.paginator.Paginator
import lila.gathering.Condition.WithVerdicts
import lila.core.team.LightTeam
import lila.swiss.{ Swiss, SwissPairing, SwissRoundNumber }

object show:

  private def fullName(s: Swiss, t: LightTeam) = s"${s.name} by ${t.name}"

  def apply(
      s: Swiss,
      team: LightTeam,
      verdicts: WithVerdicts,
      data: play.api.libs.json.JsObject,
      chatOption: Option[lila.chat.UserChat.Mine],
      streamers: List[UserId],
      isLocalMod: Boolean
  )(using ctx: PageContext): Frag =
    val isDirector       = ctx.is(s.createdBy)
    val hasScheduleInput = isDirector && s.settings.manualRounds && s.isNotFinished
    views.base.layout(
      title = fullName(s, team),
      modules = hasScheduleInput.so(EsmInit("bits.flatpickr")),
      pageModule = PageModule(
        "swiss",
        Json
          .obj(
            "data"   -> data,
            "i18n"   -> bits.jsI18n,
            "userId" -> ctx.userId,
            "chat" -> chatOption.map: c =>
              views.chat.json(
                c.chat,
                c.lines,
                name = trans.site.chatRoom.txt(),
                timeout = c.timeout,
                public = true,
                resourceId = lila.chat.Chat.ResourceId(s"swiss/${c.chat.id}"),
                localMod = isLocalMod,
                writeable = !c.locked
              ),
            "showRatings" -> ctx.pref.showRatings
          )
          .add("schedule" -> hasScheduleInput)
      ).some,
      moreCss = frag(
        cssTag("swiss.show"),
        hasScheduleInput.option(cssTag("flatpickr"))
      ),
      openGraph = OpenGraph(
        title = s"${fullName(s, team)}: ${s.variant.name} ${s.clock.show} #${s.id}",
        url = s"$netBaseUrl${routes.Swiss.show(s.id).url}",
        description =
          s"${s.nbPlayers} players compete in the ${showEnglishDate(s.startsAt)} ${s.name} Swiss tournament " +
            s"organized by ${team.name}. " +
            s.winnerId.fold("Winner is not yet decided."): winnerId =>
              s"${titleNameOrId(winnerId)} takes the prize home!"
      ).some
    )(
      main(cls := "swiss")(
        st.aside(cls := "swiss__side")(
          views.swiss.side(s, verdicts, streamers, chatOption.isDefined)
        ),
        div(cls := "swiss__main")(div(cls := "box"))
      )
    )

  def round(s: Swiss, r: SwissRoundNumber, team: LightTeam, pairings: Paginator[SwissPairing])(using
      PageContext
  ) =
    views.base.layout(
      title = s"${fullName(s, team)} • Round $r/${s.round}",
      moreCss = cssTag("swiss.show")
    ):
      val pager = pagination(p => routes.Swiss.round(s.id, p).url, r.value, s.round.value, showPost = true)
      main(cls := "box swiss__round")(
        boxTop(
          h1(
            a(href := routes.Swiss.show(s.id))(s.name),
            s" • Round $r/${s.round}"
          )
        ),
        pager(cls := "pagination--top"),
        table(cls := "slist slist-pad")(
          pairings.currentPageResults.map: p =>
            tr(cls := "paginated")(
              td(a(href := routes.Round.watcher(p.gameId, "white"), cls := "glpt")(s"#${p.gameId}")),
              td(userIdLink(p.white.some)),
              td(p.strResultOf(chess.White)),
              td(p.strResultOf(chess.Black)),
              td(userIdLink(p.black.some))
            )
        ),
        pager(cls := "pagination--bottom")
      )
