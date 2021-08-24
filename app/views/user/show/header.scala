package views.html.user.show

import lila.api.Context
import lila.app.mashup.UserInfo.Angle
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import lila.user.User

import controllers.routes

object header {

  private val dataToints = attr("data-toints")
  private val dataTab    = attr("data-tab")

  def apply(
      u: User,
      info: lila.app.mashup.UserInfo,
      angle: lila.app.mashup.UserInfo.Angle,
      social: lila.app.mashup.UserInfo.Social
  )(implicit ctx: Context) =
    frag(
      div(cls := "box__top user-show__header")(
        if (u.isPatron)
          h1(cls := s"user-link ${if (isOnline(u.id)) "online" else "offline"}")(
            a(href := routes.Plan.index)(patronIcon),
            userSpan(u, withPowerTip = false, withOnline = false)
          )
        else
          h1(userSpan(u, withPowerTip = false)),
        div(
          cls := List(
            "trophies" -> true,
            "packed"   -> (info.countTrophiesAndPerfCups > 7)
          )
        )(
          views.html.user.bits.perfTrophies(u, info.ranks),
          otherTrophies(info),
          u.plan.active option
            a(
              href := routes.Plan.index,
              cls := "trophy award patron icon3d",
              ariaTitle(s"Patron since ${showDate(u.plan.sinceDate)}")
            )(patronIconChar)
        ),
        u.disabled option span(cls := "closed")("CLOSED")
      ),
      div(cls := "user-show__social")(
        div(cls := "number-menu")(
          a(cls := "nm-item", href := routes.Relation.followers(u.username))(
            splitNumber(trans.nbFollowers.pluralSame(info.nbFollowers))
          ),
          u.noBot option a(
            href := routes.UserTournament.path(u.username, "recent"),
            cls := "nm-item tournament_stats",
            dataToints := u.toints
          )(
            splitNumber(trans.nbTournamentPoints.pluralSame(u.toints))
          ),
          a(href := routes.Study.byOwnerDefault(u.username), cls := "nm-item")(
            splitNumber(trans.`nbStudies`.pluralSame(info.nbStudies))
          ),
          a(
            cls := "nm-item",
            href := ctx.noKid option routes.ForumPost.search("user:" + u.username, 1).url
          )(
            splitNumber(trans.nbForumPosts.pluralSame(info.nbPosts))
          ),
          (ctx.isAuth && !ctx.is(u)) option
            a(cls := "nm-item note-zone-toggle")(splitNumber(s"${social.notes.size} Notes"))
        ),
        div(cls := "user-actions btn-rack")(
          (ctx is u) option frag(
            a(
              cls := "btn-rack__btn",
              href := routes.Account.profile,
              titleOrText(trans.editProfile.txt()),
              dataIcon := ""
            ),
            a(
              cls := "btn-rack__btn",
              href := routes.Relation.blocks(),
              titleOrText(trans.listBlockedPlayers.txt()),
              dataIcon := ""
            )
          ),
          isGranted(_.UserModView) option
            a(
              cls := "btn-rack__btn mod-zone-toggle",
              href := routes.User.mod(u.username),
              titleOrText("Mod zone (Hotkey: m)"),
              dataIcon := ""
            ),
          a(
            cls := "btn-rack__btn",
            href := routes.User.tv(u.username),
            titleOrText(trans.watchGames.txt()),
            dataIcon := ""
          ),
          (ctx.isAuth && !ctx.is(u)) option
            views.html.relation.actions(
              u.id,
              relation = social.relation,
              followable = social.followable,
              blocked = social.blocked
            ),
          a(
            cls := "btn-rack__btn",
            href := routes.User.download(u.username),
            titleOrText(trans.exportGames.txt()),
            dataIcon := ""
          ),
          (ctx.isAuth && ctx.noKid && !ctx.is(u)) option a(
            titleOrText(trans.reportXToModerators.txt(u.username)),
            cls := "btn-rack__btn",
            href := s"${routes.Report.form}?username=${u.username}",
            dataIcon := ""
          )
        )
      ),
      (!ctx.is(u)) option div(cls := "note-zone")(
        postForm(action := s"${routes.User.writeNote(u.username)}?note")(
          textarea(
            name := "text",
            placeholder := "Write a private note about this user"
          ),
          if (isGranted(_.ModNote))
            div(cls := "mod-note")(
              submitButton(cls := "button")(trans.send()),
              div(
                div(form3.cmnToggle("note-mod", "mod", checked = true)),
                label(`for` := "note-mod")("For moderators only")
              ),
              isGranted(_.Admin) option div(
                div(form3.cmnToggle("note-dox", "dox", checked = false)),
                label(`for` := "note-dox")("Doxing info")
              )
            )
          else
            frag(
              input(tpe := "hidden", name := "mod", value := "false"),
              submitButton(cls := "button")(trans.send())
            )
        ),
        social.notes.isEmpty option div("No note yet"),
        social.notes
          .filter { n =>
            ctx.me.exists(n.isFrom) ||
            isGranted(_.Admin) ||
            (!n.dox && isGranted(_.ModNote))
          }
          .map { note =>
            div(cls := "note")(
              p(cls := "note__text")(richText(note.text, expandImg = false)),
              p(cls := "note__meta")(
                userIdLink(note.from.some),
                br,
                note.dox option "dox ",
                momentFromNow(note.date),
                (ctx.me.exists(note.isFrom) && !note.mod) option frag(
                  br,
                  postForm(action := routes.User.deleteNote(note._id))(
                    submitButton(
                      cls := "button-empty button-red confirm button text",
                      style := "float:right",
                      dataIcon := ""
                    )("Delete")
                  )
                )
              )
            )
          }
      ),
      isGranted(_.UserModView) option div(cls := "mod-zone mod-zone-full none"),
      standardFlash(),
      angle match {
        case Angle.Games(Some(searchForm)) => views.html.search.user(u, searchForm)
        case _ =>
          val profile   = u.profileOrDefault
          val hideTroll = u.marks.troll && !ctx.is(u)
          div(id := "us_profile")(
            if (info.ratingChart.isDefined && (!u.lame || ctx.is(u) || isGranted(_.UserModView)))
              div(cls := "rating-history")(spinner)
            else
              ctx.is(u) option newPlayer(u),
            div(cls := "profile-side")(
              div(cls := "user-infos")(
                !ctx.is(u) option frag(
                  u.lame option div(cls := "warning tos_warning")(
                    span(dataIcon := "", cls := "is4"),
                    trans.thisAccountViolatedTos()
                  )
                ),
                ctx.noKid && !hideTroll option frag(
                  profile.nonEmptyRealName map { name =>
                    strong(cls := "name")(name)
                  },
                  profile.nonEmptyBio map { bio =>
                    p(cls := "bio")(richText(shorten(bio, 400), nl2br = false))
                  }
                ),
                div(cls := "stats")(
                  profile.officialRating.map { r =>
                    div(r.name.toUpperCase, " rating: ", strong(r.rating))
                  },
                  profile.nonEmptyLocation.ifTrue(ctx.noKid && !hideTroll).map { l =>
                    span(cls := "location")(l)
                  },
                  profile.countryInfo.map { c =>
                    span(cls := "country")(
                      img(cls := "flag", src := assetUrl(s"images/flags/${c.code}.png")),
                      " ",
                      c.name
                    )
                  },
                  p(cls := "thin")(trans.memberSince(), " ", showDate(u.createdAt)),
                  u.seenAt.map { seen =>
                    p(cls := "thin")(trans.lastSeenActive(momentFromNow(seen)))
                  },
                  info.completionRatePercent.map { c =>
                    p(cls := "thin")(trans.gameCompletionRate(s"$c%"))
                  },
                  ctx is u option frag(
                    a(href := routes.Account.profile, title := trans.editProfile.txt())(
                      trans.profileCompletion(s"${profile.completionPercent}%")
                    ),
                    br,
                    a(href := routes.User.opponents)(trans.favoriteOpponents())
                  ),
                  u.playTime.map { playTime =>
                    frag(
                      p(trans.tpTimeSpentPlaying(showPeriod(playTime.totalPeriod))),
                      playTime.nonEmptyTvPeriod.map { tvPeriod =>
                        p(trans.tpTimeSpentOnTV(showPeriod(tvPeriod)))
                      }
                    )
                  },
                  !hideTroll option div(cls := "social_links col2")(
                    profile.actualLinks.map { link =>
                      a(href := link.url, targetBlank, noFollow)(link.site.name)
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
                a(cls := "insight", href := routes.Insight.index(u.username), dataIcon := "")(
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
            "active"              -> (angle == Angle.Activity)
          ),
          href := routes.User.show(u.username)
        )(trans.activity.activity()),
        a(
          dataTab := "games",
          cls := List(
            "nm-item to-games" -> true,
            "active"           -> (angle.key == "games")
          ),
          href := routes.User.gamesAll(u.username)
        )(
          trans.nbGames.plural(info.user.count.game, info.user.count.game.localize),
          info.nbs.playing > 0 option
            span(
              cls := "unread",
              title := trans.nbPlaying.pluralTxt(info.nbs.playing, info.nbs.playing.localize)
            )(
              info.nbs.playing
            )
        )
      )
    )
}
