package lila.user
package ui

import lila.core.perf.UserWithPerfs
import lila.core.user.Flag
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class UserShow(helpers: Helpers, bits: UserBits):
  import helpers.{ *, given }

  def userDom(u: User)(using ctx: Context) =
    span(
      cls := userClass(u.id, none, withOnline = !u.isPatron, withPowerTip = false),
      dataHref := userUrl(u.username)
    )(
      (!u.isPatron).so(lineIcon(u)),
      titleTag(u.title),
      u.username,
      userFlair(u).map: flair =>
        if ctx.isAuth then a(href := routes.Account.profile, title := trans.site.setFlair.txt())(flair)
        else flair
    )

  def mini(
      u: UserWithPerfs,
      playing: Option[Frag],
      blocked: Boolean,
      ping: Option[Int],
      rel: Frag,
      crosstable: UserId => Option[Frag],
      flag: Option[Flag],
      best8Perfs: List[PerfKey],
      userMarks: => Frag
  )(using ctx: Context) =
    frag(
      div(cls := "upt__info")(
        div(cls := "upt__info__top")(
          userLink(u, withPowerTip = false),
          flag.map: c =>
            val titleNameSize = u.title.fold(0)(_.value.length + 1) + u.username.value.length
            val hasRoomForNameText = titleNameSize + c.shortName.length < 21
            span(
              cls := "upt__info__top__flag",
              title := (!hasRoomForNameText).option(c.name)
            )(
              img(cls := "flag", src := assetUrl(s"images/flags/${c.code}.png")),
              hasRoomForNameText.option(c.shortName)
            )
          ,
          ping.map(bits.signalBars)
        ),
        if u.lame && ctx.isnt(u) && !Granter.opt(_.UserModView)
        then div(cls := "upt__info__warning")(trans.site.thisAccountViolatedTos())
        else
          ctx.pref.showRatings.option:
            div(cls := "upt__info__ratings")(best8Perfs.map(showPerfRating(u.perfs, _)))
      ),
      ctx.userId.map: myId =>
        frag(
          (myId.isnt(u.id) && u.enabled.yes).option(
            div(cls := "upt__actions btn-rack")(
              a(
                dataIcon := Icon.AnalogTv,
                cls := "btn-rack__btn",
                title := trans.site.watchGames.txt(),
                href := routes.User.tv(u.username)
              ),
              (!blocked).option(
                frag(
                  a(
                    dataIcon := Icon.BubbleSpeech,
                    cls := "btn-rack__btn",
                    title := trans.site.chat.txt(),
                    href := routes.Msg.convo(u.username)
                  ),
                  a(
                    dataIcon := Icon.Swords,
                    cls := "btn-rack__btn",
                    title := trans.challenge.challengeToPlay.txt(),
                    href := s"${routes.Lobby.home}?user=${u.username}#friend"
                  )
                )
              ),
              rel
            )
          ),
          crosstable(myId)
        ),
      div(cls := "upt__details")(
        span(trans.site.nbGames.plural(u.count.game, u.count.game.localize)),
        span(trans.site.joinedX(momentFromNowServerText(u.createdAt))),
        (Granter.opt(_.UserModView) && (u.lameOrTroll || u.enabled.no || u.marks.rankban))
          .option(span(cls := "upt__details__marks")(userMarks))
      ),
      playing
    )

  def newPlayer(u: User)(using Translate) =
    import trans.onboarding as tro
    div(cls := "new-player")(
      h2(tro.welcomeToLichess()),
      p(tro.thisIsYourProfilePage()),
      p(
        if u.kid.yes then trans.site.kidModeIsEnabled()
        else
          tro.enabledKidModeSuggestion:
            a(href := routes.Account.kid)(trans.site.kidMode())
      ),
      p(tro.whatNowSuggestions()),
      ul(
        li(a(href := routes.Learn.index)(tro.learnChessRules())),
        li(a(href := routes.Puzzle.home)(tro.improveWithChessTacticsPuzzles())),
        li(a(href := s"${routes.Lobby.home}#ai")(tro.playTheArtificialIntelligence())),
        li(a(href := s"${routes.Lobby.home}#hook")(tro.playOpponentsFromAroundTheWorld())),
        li(a(href := routes.User.list)(tro.followYourFriendsOnLichess())),
        li(a(href := routes.Tournament.home)(tro.playInTournaments())),
        li(
          tro.learnFromXAndY(
            a(href := routes.Study.allDefault())(trans.site.toStudy()),
            a(href := routes.Video.index)(trans.learn.videos())
          )
        ),
        li(a(href := routes.Pref.form("game-display"))(tro.configureLichess())),
        li(tro.exploreTheSiteAndHaveFun())
      )
    )

  def transLocalize(key: lila.core.i18n.I18nKey, number: Int)(using Translate) =
    key.pluralSameTxt(number)

  def describeUser(user: lila.core.perf.UserWithPerfs)(using Translate) =
    import lila.rating.UserPerfsExt.bestRatedPerf
    val name = user.titleUsername
    val nbGames = user.count.game
    val createdAt = showEnglishDate(user.createdAt)
    val currentRating = user.perfs.bestRatedPerf.so: p =>
      s" Current ${p.key.perfTrans} rating: ${p.perf.intRating}."
    s"$name played $nbGames games since $createdAt.$currentRating"

  val dataUsername = attr("data-username")
