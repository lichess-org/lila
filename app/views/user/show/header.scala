package views.html.user.show

import play.api.data.Form

import lila.api.Context
import lila.app.mashup.UserInfo.Angle
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import lila.user.User

import controllers.routes

object header {

  private val dataToints = attr("data-toints")
  private val dataTab = attr("data-tab")

  def apply(
    u: User,
    info: lila.app.mashup.UserInfo,
    angle: lila.app.mashup.UserInfo.Angle,
    social: lila.app.mashup.UserInfo.Social
  )(implicit ctx: Context) = frag(
    div(cls := "box__top user-show__header")(
      h1(cls := s"user_link ${if (isOnline(u.id)) "online" else "offline"}")(
        if (u.isPatron) frag(
          a(cls := routes.Plan.index)(raw(patronIcon)),
          userSpan(u, withPowerTip = false, withOnline = false)
        )
        else userSpan(u, withPowerTip = false)
      ),
      div(cls := List(
        "trophies" -> true,
        "packed" -> (info.countTrophiesAndPerfCups > 7)
      ))(
        views.html.user.bits.perfTrophies(u, info.ranks),
        otherTrophies(u, info)
      ),
      u.plan.active option
        a(href := routes.Plan.index, cls := "trophy award patron icon3d", title := s"Patron since ${showDate(u.plan.sinceDate)}")(patronIconChar),
      u.disabled option span(cls := "closed")("CLOSED")
    ),
    div(cls := "user-show__social")(
      div(cls := "number-menu")(
        a(cls := "nm-item", href := routes.Relation.followers(u.username))(
          splitNumber(trans.nbFollowers.pluralSame(info.nbFollowers))
        ),
        info.nbBlockers.map { nb =>
          a(cls := "nm-item")(splitNumberUnsafe(nb + " Blockers"))
        },
        u.noBot option a(
          href := routes.UserTournament.path(u.username, "recent"),
          cls := "nm-item tournament_stats",
          dataToints := u.toints
        )(
            splitNumber(trans.nbTournamentPoints.pluralSame(u.toints))
          ),
        a(href := routes.Study.byOwnerDefault(u.username), cls := "nm-item")(
          splitNumberUnsafe(info.nbStudies + " studies")
        ),
        a(
          cls := "nm-item",
          href := ctx.noKid option routes.ForumPost.search("user:" + u.username, 1).url
        )(
            splitNumber(trans.nbForumPosts.pluralSame(info.nbPosts))
          ),
        (ctx.isAuth && ctx.noKid && !ctx.is(u)) option
          a(cls := "nm-item note-zone-toggle")(splitNumberUnsafe(social.notes.size + " Notes"))
      ),
      div(cls := "user_actions btn-rack")(
        (ctx is u) option frag(
          a(cls := "btn-rack__btn", href := routes.Account.profile, title := trans.editProfile.txt(), dataIcon := "%"),
          a(cls := "btn-rack__btn", href := routes.Relation.blocks(), title := trans.listBlockedPlayers.txt(), dataIcon := "k")
        ),
        isGranted(_.UserSpy) option
          a(cls := "btn-rack__btn mod_zone_toggle", href := routes.User.mod(u.username), title := "Mod zone", dataIcon := ""),
        a(cls := "btn-rack__btn", href := routes.User.tv(u.username), title := trans.watchGames.txt(), dataIcon := "1"),
        (ctx.isAuth && !ctx.is(u)) option
          views.html.relation.actions(u.id, relation = social.relation, followable = social.followable, blocked = social.blocked),
        ctx.noKid option a(
          title := trans.reportXToModerators.txt(u.username),
          cls := "btn-rack__btn",
          href := s"${routes.Report.form}?username=${u.username}",
          dataIcon := "!"
        ),
        (ctx is u) option
          a(
            cls := "btn-rack__btn",
            href := routes.Game.exportByUser(u.username),
            title := trans.exportGames.txt(),
            dataIcon := "x"
          )
      )
    ),
    (ctx.noKid && !ctx.is(u)) option div(cls := "note-zone")(
      form(action := s"${routes.User.writeNote(u.username)}?note", method := "post")(
        textarea(name := "text", placeholder := "Write a note about this user only you and your friends can read"),
        button(tpe := "submit", cls := "button")(trans.send.frag()),
        if (isGranted(_.ModNote)) label(style := "margin-left: 1em;")(
          input(tpe := "checkbox", name := "mod", checked := "true", value := "true", style := "vertical-align: middle;"),
          "For moderators only"
        )
        else input(tpe := "hidden", name := "mod", value := "false")
      ),
      social.notes.isEmpty option div("No note yet"),
      social.notes.map { note =>
        div(cls := "note")(
          p(cls := "text")(richText(note.text)),
          p(cls := "meta")(
            userIdLink(note.from.some),
            br,
            momentFromNow(note.date),
            (ctx.me.exists(note.isFrom) && !note.mod) option frag(
              br,
              form(action := routes.User.deleteNote(note._id), method := "post")(
                button(tpe := "submit", cls := "thin confirm button text", style := "float:right", dataIcon := "q")("Delete")
              )
            )
          )
        )
      }
    ),
    ((ctx is u) && u.perfs.bestStandardRating > 2400 && !u.hasTitle && !ctx.pref.hasSeenVerifyTitle) option claimTitle(u),
    isGranted(_.UserSpy) option div(cls := "mod_zone none"),
    angle match {
      case Angle.Games(Some(searchForm)) => views.html.search.user(u, searchForm)
      case _ =>
        val profile = u.profileOrDefault
        div(id := "us_profile")(
          info.ratingChart.ifTrue(!u.lame || ctx.is(u) || isGranted(_.UserSpy)).map { ratingChart =>
            div(cls := "rating_history")(spinner)
          } getOrElse {
            ctx.is(u) option newPlayer(u)
          },
          div(cls := "profile-side")(
            div(cls := "user-infos scroll-shadow-hard")(
              !ctx.is(u) option frag(
                u.engine option div(cls := "warning engine_warning")(
                  span(dataIcon := "j", cls := "is4"),
                  trans.thisPlayerUsesChessComputerAssistance.frag()
                ),
                (u.booster && (u.count.game > 0 || isGranted(_.Hunter))) option div(cls := "warning engine_warning")(
                  span(dataIcon := "j", cls := "is4"),
                  trans.thisPlayerArtificiallyIncreasesTheirRating.frag(),
                  (u.count.game == 0) option """
Only visible to mods. A booster mark without any games is a way to
prevent a player from ever playing (except against boosters/cheaters).
It's useful against spambots. These marks are not visible to the public."""
                )
              ),
              ctx.noKid option frag(
                profile.nonEmptyRealName.map { name =>
                  strong(cls := "name")(name)
                },
                profile.nonEmptyBio.ifTrue(!u.troll || ctx.is(u)).map { bio =>
                  p(cls := "bio")(richText(shorten(bio, 400), nl2br = false))
                }
              ),
              div(cls := "stats")(
                profile.officialRating.map { r =>
                  div(r.name.toUpperCase, " rating: ", strong(r.rating))
                },
                profile.nonEmptyLocation.ifTrue(ctx.noKid).map { l =>
                  span(cls := "location")(l)
                },
                profile.countryInfo.map { c =>
                  span(cls := "country")(
                    img(cls := "flag", src := staticUrl(s"images/flags/${c.code}.png")),
                    " ",
                    c.name
                  )
                },
                p(cls := "thin")(trans.memberSince.frag(), " ", showDate(u.createdAt)),
                u.seenAt.map { seen =>
                  p(cls := "thin")(trans.lastSeenActive.frag(momentFromNow(seen)))
                },
                info.completionRatePercent.map { c =>
                  p(cls := "thin")(trans.gameCompletionRate.frag(s"$c%"))
                },
                (ctx is u) option frag(
                  a(href := routes.Account.profile, title := trans.editProfile.txt())(
                    trans.profileCompletion.frag(s"${profile.completionPercent}%")
                  ),
                  br,
                  a(href := routes.User.opponents)(trans.favoriteOpponents.frag())
                ),
                info.playTime.map { playTime =>
                  frag(
                    br, br,
                    p(trans.tpTimeSpentPlaying.frag(showPeriod(playTime.totalPeriod))),
                    playTime.nonEmptyTvPeriod.map { tvPeriod =>
                      p(trans.tpTimeSpentOnTV.frag(showPeriod(tvPeriod)))
                    }
                  )
                },
                div(cls := "social_links col2")(
                  profile.actualLinks.map { link =>
                    a(href := link.url, target := "_blank", rel := "no-follow")(link.site.name)
                  }
                ),
                div(cls := "teams col2")(
                  info.teamIds.sorted.map { t =>
                    teamLink(t, withIcon = false)
                  }
                )
              )
            ),
            info.insightVisible option
              a(cls := "insight", href := routes.Insight.index(u.username), dataIcon := "7")(
                span(
                  strong("Chess Insights"),
                  em("Analytics from ", if (ctx.is(u)) "your" else s"${u.username}'s", " games")
                )
              )
          )
        )
    },
    div(cls := "angles number-menu number-menu--tabs menu-box-pop")(
      a(
        dataTab := "activity",
        cls := List(
          "nm-item to-activity" -> true,
          "active" -> (angle == Angle.Activity)
        ),
        href := routes.User.show(u.username)
      )(trans.activity.activity.frag()),
      a(
        dataTab := "games",
        cls := List(
          "nm-item to-games" -> true,
          "active" -> (angle.key == "games")
        ),
        href := routes.User.gamesAll(u.username)
      )(
          trans.nbGames.pluralFrag(info.user.count.game, info.user.count.game.localize),
          info.nbs.playing > 0 option
            span(cls := "unread", title := trans.nbPlaying.pluralTxt(info.nbs.playing, info.nbs.playing.localize))(
              info.nbs.playing
            )
        )
    )
  )
}

