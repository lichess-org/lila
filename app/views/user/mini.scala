package views.html.user

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.user.User

import controllers.routes

object mini:

  def apply(
      u: User.WithPerfs,
      playing: Option[lila.game.Pov],
      blocked: Boolean,
      followable: Boolean,
      rel: Option[lila.relation.Relation],
      ping: Option[Int],
      crosstable: Option[lila.game.Crosstable]
  )(using ctx: Context) =
    frag(
      div(cls := "upt__info")(
        div(cls := "upt__info__top")(
          div(cls := "left")(
            userLink(u, withPowerTip = false),
            u.profileOrDefault.countryInfo map { c =>
              val hasRoomForNameText = u.username.length + c.shortName.length < 20
              span(
                cls   := "upt__info__top__country",
                title := (!hasRoomForNameText).option(c.name)
              )(
                img(cls := "flag", src := assetUrl(s"images/flags/${c.code}.png")),
                hasRoomForNameText option c.shortName
              )
            }
          ),
          ping map bits.signalBars
        ),
        if u.lame && !ctx.is(u) && !isGranted(_.UserModView)
        then div(cls := "upt__info__warning")(trans.thisAccountViolatedTos())
        else
          ctx.pref.showRatings option div(cls := "upt__info__ratings"):
            u.perfs.best8Perfs.map(showPerfRating(u.perfs, _))
      ),
      ctx.userId.map: myId =>
        frag(
          (myId != u.id && u.enabled.yes) option div(cls := "upt__actions btn-rack")(
            a(
              dataIcon := licon.AnalogTv,
              cls      := "btn-rack__btn",
              title    := trans.watchGames.txt(),
              href     := routes.User.tv(u.username)
            ),
            !blocked option frag(
              a(
                dataIcon := licon.BubbleSpeech,
                cls      := "btn-rack__btn",
                title    := trans.chat.txt(),
                href     := routes.Msg.convo(u.username)
              ),
              a(
                dataIcon := licon.Swords,
                cls      := "btn-rack__btn",
                title    := trans.challenge.challengeToPlay.txt(),
                href     := s"${routes.Lobby.home}?user=${u.username}#friend"
              )
            ),
            views.html.relation.mini(u.id, blocked, followable, rel)
          ),
          crosstable.flatMap(_.nonEmpty) map { cross =>
            a(
              cls   := "upt__score",
              href  := s"${routes.User.games(u.username, "me")}#games",
              title := trans.nbGames.pluralTxt(cross.nbGames, cross.nbGames.localize)
            ):
              trans.yourScore(raw:
                val opponent = ~cross.showOpponentScore(myId)
                s"""<strong>${cross.showScore(myId)}</strong> - <strong>$opponent</strong>"""
              )
          }
        ),
      isGranted(_.UserModView) option div(cls := "upt__mod")(
        span(
          trans.nbGames.plural(u.count.game, u.count.game.localize),
          " ",
          momentFromNowOnce(u.createdAt)
        ),
        (u.lameOrTroll || u.enabled.no) option span(cls := "upt__mod__marks")(mod.userMarks(u.user, None))
      ),
      playing.ifFalse(ctx.pref.isBlindfold).map(views.html.game.mini(_))
    )
