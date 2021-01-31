package views.html
package swiss

import controllers.routes
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.swiss.{ Swiss, SwissCondition }

object show {

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
      title = s"${s.name} #${s.id}",
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
          title = s"${s.name}: ${s.variant.name} ${s.clock.show} #${s.id}",
          url = s"$netBaseUrl${routes.Swiss.show(s.id.value).url}",
          description =
            s"${s.nbPlayers} players compete in the ${showEnglishDate(s.startsAt)} ${s.name} swiss tournament " +
              s"organized by ${teamIdToName(s.teamId)}. " +
              s.winnerId.fold("Winner is not yet decided.") { winnerId =>
                s"${usernameOrId(winnerId)} takes the prize home!"
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
}
