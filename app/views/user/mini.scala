package views.html.user

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object mini {

  def apply(
      u: User,
      playing: Option[lila.game.Pov],
      blocked: Boolean,
      followable: Boolean,
      rel: Option[lila.relation.Relation],
      ping: Option[Int],
      crosstable: Option[lila.game.Crosstable]
  )(implicit ctx: Context) =
    frag(
      div(cls := "upt__info")(
        div(cls := "upt__info__top")(
          div(cls := "left")(
            userLink(u, withPowerTip = false),
            u.profileOrDefault.countryInfo map { c =>
              val hasRoomForNameText = u.username.length + c.shortName.length < 20
              span(
                cls := "upt__info__top__country",
                title := (!hasRoomForNameText).option(c.name)
              )(
                img(cls := "flag", src := assetUrl(s"images/flags/${c.code}.png")),
                hasRoomForNameText option c.shortName
              )
            }
          ),
          ping map bits.signalBars
        ),
        if (u.marks.engine && !ctx.me.has(u) && !isGranted(_.UserModView))
          div(cls := "upt__info__warning")(trans.thisAccountViolatedTos())
        else
          div(cls := "upt__info__ratings")(u.best8Perfs map { showPerfRating(u, _) })
      ),
      ctx.userId map { myId =>
        frag(
          (myId != u.id && u.enabled) option div(cls := "upt__actions btn-rack")(
            a(
              dataIcon := "1",
              cls := "btn-rack__btn",
              title := trans.watchGames.txt(),
              href := routes.User.tv(u.username)
            ),
            !blocked option frag(
              a(
                dataIcon := "c",
                cls := "btn-rack__btn",
                title := trans.chat.txt(),
                href := routes.Msg.convo(u.username)
              ),
              a(
                dataIcon := "U",
                cls := "btn-rack__btn",
                title := trans.challenge.challengeToPlay.txt(),
                href := s"${routes.Lobby.home}?user=${u.username}#friend"
              )
            ),
            views.html.relation.mini(u.id, blocked, followable, rel)
          ),
          crosstable.flatMap(_.nonEmpty) map { cross =>
            a(
              cls := "upt__score",
              href := s"${routes.User.games(u.username, "me")}#games",
              title := trans.nbGames.pluralTxt(cross.nbGames, cross.nbGames.localize)
            )(trans.yourScore(raw(s"""<strong>${cross.showScore(myId)}</strong> - <strong>${~cross
              .showOpponentScore(myId)}</strong>""")))
          }
        )
      },
      isGranted(_.UserModView) option div(cls := "upt__mod")(
        span(
          trans.nbGames.plural(u.count.game, u.count.game.localize),
          " ",
          momentFromNowOnce(u.createdAt)
        ),
        (u.lameOrTroll || u.disabled) option span(cls := "upt__mod__marks")(mod.userMarks(u, None))
      ),
      playing.ifFalse(ctx.pref.isBlindfold).map {
        views.html.game.mini(_)
      }
    )
}
