package views.html
package tournament

import play.api.libs.json.Json

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
    moreJs = frag(
      jsAt(s"compiled/lidraughts.tournament${isProd ?? (".min")}.js"),
      embedJsUnsafe(s"""lidraughts=lidraughts||{};lidraughts.tournament=${
        safeJsonValue(Json.obj(
          "data" -> data,
          "i18n" -> bits.jsI18n(),
          "userId" -> ctx.userId,
          "chat" -> chatOption.map { c =>
            chat.json(
              c.chat,
              name = trans.chatRoom.txt(),
              timeout = c.timeout,
              public = true,
              resourceId = lidraughts.chat.Chat.ResourceId(s"tournament/${c.chat.id}")
            )
          }
        ))
      }""")
    ),
    moreCss = cssTag("tournament.show"),
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
        st.aside(cls := "tour__side")(tournament.side(tour, verdicts, streamers, shieldOwner, chatOption.isDefined)),
        div(cls := "tour__main")(div(cls := "box")),
        tour.isCreated option div(cls := "tour__faq")(
          faq(tour.mode.rated.some, tour.system.some, tour.isPrivate.option(tour.id))
        )
      )
    ))
}
