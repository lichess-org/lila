package views.html
package tournament

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.tournament.Tournament
import lila.user.User

import controllers.routes

object show {

  def apply(
      tour: Tournament,
      verdicts: lila.tournament.Condition.All.WithVerdicts,
      data: play.api.libs.json.JsObject,
      chatOption: Option[lila.chat.UserChat.Mine],
      streamers: List[User.ID],
      shieldOwner: Option[lila.tournament.TournamentShield.OwnerId]
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${tour.name()} #${tour.id}",
      moreJs = frag(
        jsModule("tournament"),
        embedJsUnsafe(s"""lishogi=lishogi||{};lishogi.tournament=${safeJsonValue(
            Json.obj(
              "data"   -> data,
              "i18n"   -> bits.jsI18n(tour),
              "userId" -> ctx.userId,
              "chat" -> chatOption.map { c =>
                chat.json(
                  c.chat,
                  name = trans.chatRoom.txt(),
                  timeout = c.timeout,
                  public = true,
                  resourceId = lila.chat.Chat.ResourceId(s"tournament/${c.chat.id}")
                )
              }
            )
          )}""")
      ),
      moreCss = cssTag {
        if (tour.isTeamBattle) "tournament.show.team-battle"
        else "tournament.show"
      },
      shogiground = false,
      openGraph = lila.app.ui
        .OpenGraph(
          title =
            s"${tour.name()}: ${variantName(tour.variant)} ${tour.clock.show} ${modeName(tour.mode)} #${tour.id}",
          url = s"$netBaseUrl${routes.Tournament.show(tour.id).url}",
          description = s"${showDate(tour.startsAt)} - ${tour.name()} - ${trans.nbPlayers
              .pluralSameTxt(tour.nbPlayers)}, " +
            s"${trans.duration.txt().toLowerCase}: ${tour.minutes}m. " +
            tour.winnerId.fold(trans.winnerIsNotYetDecided.txt()) { winnerId =>
              trans.xWon.txt(usernameOrId(winnerId))
            } // Jun 19, 2023 - SuperBlitz Arena - 377 players, duration: 57m. Winner is not yet decided.
        )
        .some
    )(
      main(cls := s"tour${tour.schedule
          .?? { sched =>
            s" tour-sched tour-sched-${sched.freq.name} tour-speed-${sched.speed.name} tour-variant-${sched.variant.key} tour-id-${tour.id}"
          }}")(
        st.aside(cls := "tour__side")(
          tournament.side(tour, verdicts, streamers, shieldOwner, chatOption.isDefined)
        ),
        div(cls := "tour__main")(div(cls := "box")),
        tour.isCreated option div(cls := "tour__faq")(
          faq(tour.mode.rated.some, tour.isPrivate.option(tour.id))
        )
      )
    )
}
