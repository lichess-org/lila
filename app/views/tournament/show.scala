package views.html
package tournament

import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.tournament.Tournament

import controllers.routes

object show {

  def apply(
    tour: Tournament,
    verdicts: lila.tournament.Condition.All.WithVerdicts,
    data: play.api.libs.json.JsObject,
    chatOption: Option[lila.chat.UserChat.Mine],
    streamers: Set[lila.user.User.ID],
    shieldOwner: Option[lila.tournament.TournamentShield.OwnerId]
  )(implicit ctx: Context) = bits.layout(
    title = s"${tour.fullName} #${tour.id}",
    side = Some(tournament.side(tour, verdicts, streamers, shieldOwner)),
    chat = chat.frag.some,
    underchat = Some(div(
      cls := "watchers hidden",
      aria.live := "off",
      aria.relevant := "additions removals text"
    )(span(cls := "list inline_userlist"))),
    moreJs = frag(
      jsAt(s"compiled/lichess.tournament${isProd ?? (".min")}.js"),
      embedJs(s"""lichess = lichess || {}; lichess.tournament = {
data: ${safeJsonValue(data)},
i18n: ${jsI18n()},
userId: ${jsUserIdString},
chat: ${
        chatOption.fold("null")(c =>
          safeJsonValue(chat.json(c.chat, name = trans.chatRoom.txt(), timeout = c.timeout, public = true)))
      }};""")
    ),
    moreCss = cssTags(List("chat.css" -> true, "quote.css" -> tour.isCreated)),
    chessground = false,
    openGraph = lila.app.ui.OpenGraph(
      title = s"${tour.fullName}: ${tour.variant.name} ${tour.clock.show} ${tour.mode.name} #${tour.id}",
      url = s"$netBaseUrl${routes.Tournament.show(tour.id).url}",
      description = s"${tour.nbPlayers} players compete in the ${showEnglishDate(tour.startsAt)} ${tour.fullName}. " +
        s"${tour.clock.show} ${tour.mode.name} games are played during ${tour.minutes} minutes. " +
        tour.winnerId.fold("Winner is not yet decided.") { winnerId =>
          s"${usernameOrId(winnerId)} takes the prize home!"
        }
    ).some
  )(frag(
      div(id := "tournament", cls := tour.schedule.map { sched =>
        s"scheduled ${sched.freq.name} ${sched.speed.name} ${sched.variant.key} id_${tour.id}"
      })(
        div(cls := "content_box no_padding tournament_box tournament_show")(
          div(cls := "content_box_content")(spinner)
        )
      ),
      tour.isCreated option div(id := "tournament_faq", cls := "none")(
        faq(tour.mode.rated.some, tour.system.some, tour.isPrivate.option(tour.id))
      )
    ))
}
