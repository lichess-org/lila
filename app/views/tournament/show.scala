package views.html
package tournament

import play.twirl.api.Html

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue
import lidraughts.tournament.Tournament

import controllers.routes

object show {

  def apply(
    tour: Tournament,
    verdicts: lidraughts.tournament.Condition.All.WithVerdicts,
    data: play.api.libs.json.JsObject,
    chatOption: Option[lidraughts.chat.UserChat.Mine],
    streamers: Set[lidraughts.user.User.ID],
    shieldOwner: Option[lidraughts.tournament.TournamentShield.OwnerId]
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"${tour.fullName} #${tour.id}",
    underchat = Some(div(
      cls := "watchers hidden",
      aria.live := "off",
      aria.relevant := "additions removals text"
    )(span(cls := "list inline_userlist"))),
    moreJs = frag(
      jsAt(s"compiled/lidraughts.tournament${isProd ?? (".min")}.js"),
      embedJs(s"""lidraughts=lidraughts||{};lidraughts.tournament={
data:${safeJsonValue(data)},
i18n:${jsI18n()},
userId:${jsUserIdString},
chat:${
        chatOption.fold("null")(c =>
          safeJsonValue(chat.json(c.chat, name = trans.chatRoom.txt(), timeout = c.timeout, public = true)))
      }};""")
    ),
    moreCss = responsiveCssTag("tournament.show"),
    responsive = true,
    draughtsground = false,
    openGraph = lidraughts.app.ui.OpenGraph(
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
        st.aside(cls := "tour__side")(tournament.side(tour, verdicts, streamers, shieldOwner)),
        chatOption.map(_ => views.html.chat.frag),
        div(cls := "tour__main box"),
        div(cls := "tour__featured"),
        div(cls := "tour__player box"),
        tour.isCreated option div(cls := "tour__faq")(
          faq(tour.mode.rated.some, tour.system.some, tour.isPrivate.option(tour.id))
        )
      ),
      div(cls := "tour__underchat none")(views.html.game.bits.watchers)
    ))
}
