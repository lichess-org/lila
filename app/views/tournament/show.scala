package views.html
package tournament

import controllers.routes
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tournament.Tournament
import lila.user.User

object show {

  def apply(
      tour: Tournament,
      verdicts: lila.tournament.Condition.All.WithVerdicts,
      data: play.api.libs.json.JsObject,
      chatOption: Option[lila.chat.UserChat.Mine],
      streamers: List[User.ID],
      shieldOwner: Option[lila.tournament.TournamentShield.OwnerId],
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${tournamentName(tour)} #${tour.id}",
      moreJs = frag(
        moduleJsTag(
          "tournament",
          Json.obj(
            "data"   -> data,
            "userId" -> ctx.userId,
            "chat" -> chatOption.map { c =>
              chat.json(
                c.chat,
                name = trans.chatRoom.txt(),
                timeout = c.timeout,
                public = true,
                resourceId = lila.chat.Chat.ResourceId(s"tournament/${c.chat.id}"),
              )
            },
          ),
        ),
      ),
      moreCss = cssTag {
        if (tour.isTeamBattle) "tournament.show.team-battle"
        else "tournament.show"
      },
      shogiground = false,
      openGraph = lila.app.ui
        .OpenGraph(
          title =
            s"${tournamentName(tour)}: ${variantName(tour.variant)} ${tour.timeControl.show} ${modeName(tour.mode)} #${tour.id}",
          url = s"$netBaseUrl${routes.Tournament.show(tour.id).url}",
          description = s"${showDate(tour.startsAt)} - ${tournamentName(tour)} - ${trans.nbPlayers
              .pluralSameTxt(tour.nbPlayers)}, " +
            s"${trans.duration.txt().toLowerCase}: ${tour.minutes}m. " +
            tour.winnerId.fold(trans.winnerIsNotYetDecided.txt()) { winnerId =>
              trans.xWon.txt(usernameOrId(winnerId))
            }, // Jun 19, 2023 - SuperBlitz Arena - 377 players, duration: 57m. Winner is not yet decided.
        )
        .some,
    )(
      main(cls := s"tour${tour.schedule
          .?? { sched =>
            s" tour-sched tour-sched-${sched.freq.key} tour-speed-${sched.speed.key} tour-variant-${sched.variant.key} tour-id-${tour.id}"
          }}")(
        st.aside(cls := "tour__side")(
          tournament.side(tour, verdicts, streamers, shieldOwner, chatOption.isDefined),
        ),
        div(cls := "tour__main")(div(cls := "box")),
        (tour.isCreated || (tour.hasArrangements && !tour.isFinished)) option div(
          cls := "tour__faq",
        )(
          faq(tour.format, tour.mode.rated.some, tour.isPrivate.option(tour.id)),
        ),
      ),
    )
}
