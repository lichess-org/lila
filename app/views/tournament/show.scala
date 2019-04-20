package views.html
package tournament

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
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"${tour.fullName} #${tour.id}",
    moreJs = frag(
      jsAt(s"compiled/lichess.tournament${isProd ?? (".min")}.js"),
      embedJs(s"""lichess=lichess||{};lichess.tournament={
data:${safeJsonValue(data)},
i18n:${bits.jsI18n()},
userId:${jsUserIdString},
chat:${
        chatOption.fold("null")(c =>
          safeJsonValue(chat.json(c.chat, name = trans.chatRoom.txt(), timeout = c.timeout, public = true)))
      }};""")
    ),
    moreCss = responsiveCssTag("tournament.show"),
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
      main(cls := s"tour${
        tour.schedule.?? { sched =>
          s" tour-sched tour-sched-${sched.freq.name} tour-speed-${sched.speed.name} tour-variant-${sched.variant.key} tour-id-${tour.id}"
        }
      }")(
        st.aside(cls := "tour__side")(tournament.side(tour, verdicts, streamers, shieldOwner, chatOption.isDefined)),
        div(cls := "tour__main box"),
        tour.isCreated option div(cls := "tour__faq box box-pad")(
          faq(tour.mode.rated.some, tour.system.some, tour.isPrivate.option(tour.id))
        )
      )
    ))
}
