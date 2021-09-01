package views.html
package swiss

import controllers.routes
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.swiss.{ Swiss, SwissCondition }
import lila.swiss.SwissRound
import lila.common.paginator.Paginator
import lila.swiss.SwissPairing

object show {

  private def fullName(s: Swiss) = s"${s.name} by ${teamIdToName(s.teamId)}"

  def apply(
      s: Swiss,
      verdicts: SwissCondition.All.WithVerdicts,
      data: play.api.libs.json.JsObject,
      chatOption: Option[lila.chat.UserChat.Mine],
      streamers: List[lila.user.User.ID],
      isLocalMod: Boolean
  )(implicit ctx: Context): Frag = {
    val isDirector       = ctx.userId.has(s.createdBy)
    val hasScheduleInput = isDirector && s.settings.manualRounds && s.isNotFinished
    views.html.base.layout(
      title = fullName(s),
      moreJs = frag(
        jsModule("swiss"),
        hasScheduleInput option jsModule("flatpickr"),
        embedJsUnsafeLoadThen(s"""LichessSwiss.start(${safeJsonValue(
          Json
            .obj(
              "data"   -> data,
              "i18n"   -> bits.jsI18n,
              "userId" -> ctx.userId,
              "chat" -> chatOption.map { c =>
                chat.json(
                  c.chat,
                  name = trans.chatRoom.txt(),
                  timeout = c.timeout,
                  public = true,
                  resourceId = lila.chat.Chat.ResourceId(s"swiss/${c.chat.id}"),
                  localMod = isLocalMod
                )
              }
            )
            .add("schedule" -> hasScheduleInput)
        )})""")
      ),
      moreCss = frag(
        cssTag("swiss.show"),
        hasScheduleInput option cssTag("flatpickr")
      ),
      chessground = false,
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"${fullName(s)}: ${s.variant.name} ${s.clock.show} #${s.id}",
          url = s"$netBaseUrl${routes.Swiss.show(s.id.value).url}",
          description =
            s"${s.nbPlayers} players compete in the ${showEnglishDate(s.startsAt)} ${s.name} swiss tournament " +
              s"organized by ${teamIdToName(s.teamId)}. " +
              s.winnerId.fold("Winner is not yet decided.") { winnerId =>
                s"${titleNameOrId(winnerId)} takes the prize home!"
              }
        )
        .some
    )(
      main(cls := "swiss")(
        st.aside(cls := "swiss__side")(
          swiss.side(s, verdicts, streamers, chatOption.isDefined)
        ),
        div(cls := "swiss__main")(div(cls := "box"))
      )
    )
  }

  def round(s: Swiss, r: SwissRound.Number, pairings: Paginator[SwissPairing])(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${fullName(s)} • Round $r/${s.round}",
      moreCss = cssTag("swiss.show"),
      moreJs = infiniteScrollTag
    ) {
      val pager = views.html.base.bits
        .pagination(p => routes.Swiss.round(s.id.value, p).url, r.value, s.round.value, showPost = true)
      main(cls := "box swiss__round")(
        h1(
          a(href := routes.Swiss.show(s.id.value))(s.name),
          s" • Round $r/${s.round}"
        ),
        pager(cls := "pagination--top"),
        table(cls := "slist slist-pad")(
          tbody(cls := "infinite-scroll")(
            pairings.currentPageResults map { p =>
              tr(cls := "paginated")(
                td(a(href := routes.Round.watcher(p.gameId, "white"), cls := "glpt")(s"#${p.gameId}")),
                td(userIdLink(p.white.some)),
                td(p strResultOf chess.White),
                td(p strResultOf chess.Black),
                td(userIdLink(p.black.some))
              )
            },
            pagerNextTable(pairings, p => routes.Swiss.round(s.id.value, r.value).url)
          )
        ),
        pager(cls := "pagination--bottom")
      )
    }
}
