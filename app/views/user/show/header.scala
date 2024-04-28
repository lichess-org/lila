package views.user
package show

import lila.app.mashup.UserInfo
import lila.app.templating.Environment.{ *, given }

import lila.common.String.html.richText
import lila.user.Plan.sinceDate
import lila.user.Profile.*
import lila.user.PlayTime.*

object header:

  private val dataToints = attr("data-toints")
  private val dataTab    = attr("data-tab")

  def apply(u: User, info: UserInfo, angle: UserInfo.Angle, social: UserInfo.Social)(using ctx: Context) =
    frag(
      div(cls := "box__top user-show__header")(
        if u.isPatron then
          h1(cls := s"user-link ${if isOnline(u.id) then "online" else "offline"}")(
            a(href := routes.Plan.index)(patronIcon),
            ui.userDom(u)
          )
        else h1(ui.userDom(u)),
        div(
          cls := List(
            "trophies" -> true,
            "packed"   -> (info.trophies.countTrophiesAndPerfCups > 7)
          )
        )(
          views.user.bits.perfTrophies(u, info.ranks),
          otherTrophies(info),
          u.plan.active.option(
            a(
              href := routes.Plan.index,
              cls  := "trophy award patron icon3d",
              ariaTitle(s"Patron since ${showDate(u.plan.sinceDate)}")
            )(patronIconChar)
          )
        ),
        u.enabled.no.option(span(cls := "closed")("CLOSED"))
      ),
      div(cls := "user-show__social")(
        div(cls := "number-menu")(
          u.noBot.option(
            a(
              href       := routes.UserTournament.path(u.username, "recent"),
              cls        := "nm-item",
              dataToints := u.toints
            )(
              splitNumber(trans.site.nbTournamentPoints.pluralSame(u.toints))
            )
          ),
          (info.nbSimuls > 0).option(
            a(
              href := routes.Simul.byUser(u.username),
              cls  := "nm-item"
            )(
              splitNumber(trans.site.nbSimuls.pluralSame(info.nbSimuls))
            )
          ),
          (info.nbRelays > 0).option(
            a(
              href := routes.RelayTour.by(u.username),
              cls  := "nm-item"
            )(
              splitNumber(trans.broadcast.nbBroadcasts.pluralSame(info.nbRelays))
            )
          ),
          a(href := routes.Study.byOwnerDefault(u.username), cls := "nm-item")(
            splitNumber(trans.site.`nbStudies`.pluralSame(info.nbStudies))
          ),
          ctx.kid.no.option(
            a(
              cls  := "nm-item",
              href := routes.ForumPost.search("user:" + u.username, 1).url
            )(
              splitNumber(trans.site.nbForumPosts.pluralSame(info.nbForumPosts))
            )
          ),
          (ctx.kid.no && (info.ublog.exists(_.nbPosts > 0) || ctx.is(u))).option(
            a(
              cls  := "nm-item",
              href := routes.Ublog.index(u.username)
            )(
              splitNumber(s"${info.ublog.so(_.nbPosts)} blog posts")
            )
          ),
          (ctx.isAuth && ctx.isnt(u))
            .option(a(cls := "nm-item note-zone-toggle")(splitNumber(s"${social.notes.size} Notes")))
        ),
        div(cls := "user-actions btn-rack")(
          (ctx
            .is(u))
            .option(
              frag(
                a(
                  cls  := "btn-rack__btn",
                  href := routes.Account.profile,
                  titleOrText(trans.site.editProfile.txt()),
                  dataIcon := Icon.Gear
                ),
                a(
                  cls  := "btn-rack__btn",
                  href := routes.Relation.blocks(),
                  titleOrText(trans.site.listBlockedPlayers.txt()),
                  dataIcon := Icon.NotAllowed
                )
              )
            ),
          isGranted(_.UserModView).option(
            a(
              cls  := "btn-rack__btn mod-zone-toggle",
              href := routes.User.mod(u.username),
              titleOrText("Mod zone (Hotkey: m)"),
              dataIcon := Icon.Agent
            )
          ),
          a(
            cls  := "btn-rack__btn",
            href := routes.User.tv(u.username),
            titleOrText(trans.site.watchGames.txt()),
            dataIcon := Icon.AnalogTv
          ),
          ctx
            .isnt(u)
            .option(
              views.relation.actions(
                u.light,
                relation = social.relation,
                followable = social.followable,
                blocked = social.blocked
              )
            ),
          a(
            cls  := "btn-rack__btn",
            href := s"${routes.UserAnalysis.index}#explorer/${u.username}",
            titleOrText(trans.site.openingExplorer.txt()),
            dataIcon := Icon.Book
          ),
          a(
            cls  := "btn-rack__btn",
            href := routes.User.download(u.username),
            titleOrText(trans.site.exportGames.txt()),
            dataIcon := Icon.Download
          ),
          (ctx.isAuth && ctx.kid.no && ctx.isnt(u)).option(
            a(
              titleOrText(trans.site.reportXToModerators.txt(u.username)),
              cls      := "btn-rack__btn",
              href     := s"${routes.Report.form}?username=${u.username}",
              dataIcon := Icon.CautionTriangle
            )
          )
        )
      ),
      ctx.isnt(u).option(noteUi.zone(u, social.notes)),
      isGranted(_.UserModView).option(div(cls := "mod-zone mod-zone-full none")),
      standardFlash,
      angle match
        case UserInfo.Angle.Games(Some(searchForm)) => views.search.user(u, searchForm)
        case _ =>
          val profile   = u.profileOrDefault
          val hideTroll = u.marks.troll && ctx.isnt(u)
          div(id := "us_profile")(
            if info.ratingChart.isDefined && (!u.lame || ctx.is(u) || isGranted(_.UserModView)) then
              views.user.perfStat.ui.ratingHistoryContainer
            else (ctx.is(u) && u.count.game < 10).option(ui.newPlayer(u)),
            div(cls := "profile-side")(
              div(cls := "user-infos")(
                ctx
                  .isnt(u)
                  .option(
                    frag(
                      u.lame.option(
                        div(cls := "warning tos_warning")(
                          span(dataIcon := Icon.CautionCircle, cls := "is4"),
                          trans.site.thisAccountViolatedTos()
                        )
                      )
                    )
                  ),
                (ctx.kid.no && !hideTroll && ctx.kid.no).option(
                  frag(
                    profile.nonEmptyRealName.map { name =>
                      strong(cls := "name")(name)
                    },
                    profile.nonEmptyBio.map { bio =>
                      p(cls := "bio")(richText(shorten(bio, 400), nl2br = false))
                    }
                  )
                ),
                div(cls := "stats")(
                  profile.officialRating.map: r =>
                    div(r.name.toUpperCase, " rating: ", strong(r.rating)),
                  profile.nonEmptyLocation.ifTrue(ctx.kid.no && !hideTroll).map { l =>
                    span(cls := "location")(l)
                  },
                  profile.flagInfo.map: c =>
                    span(cls := "flag")(
                      img(src := assetUrl(s"images/flags/${c.code}.png")),
                      " ",
                      c.name
                    ),
                  p(cls := "thin")(trans.site.memberSince(), " ", showDate(u.createdAt)),
                  u.seenAt.map: seen =>
                    p(cls := "thin")(trans.site.lastSeenActive(momentFromNow(seen))),
                  ctx
                    .is(u)
                    .option(
                      a(href := routes.Account.profile, title := trans.site.editProfile.txt())(
                        trans.site.profileCompletion(s"${profile.completionPercent}%")
                      )
                    ),
                  (ctx.is(u) || isGranted(_.CloseAccount)).option(
                    frag(
                      br,
                      a(href := routes.Relation.following(u.username))(trans.site.friends())
                    )
                  ),
                  (ctx.is(u) || isGranted(_.BoostHunter)).option(
                    frag(
                      br,
                      a(href := s"${routes.User.opponents}?u=${u.username}")(trans.site.favoriteOpponents())
                    )
                  ),
                  u.playTime.map: playTime =>
                    frag(
                      p(
                        trans.site.tpTimeSpentPlaying(
                          lila.core.i18n.translateDuration(playTime.totalDuration)
                        )
                      ),
                      playTime.nonEmptyTvDuration.map: tvDuration =>
                        p(trans.site.tpTimeSpentOnTV(lila.core.i18n.translateDuration(tvDuration)))
                    ),
                  (!hideTroll).option(
                    div(cls := "social_links col2")(
                      profile.actualLinks.nonEmpty.option(strong(trans.site.socialMediaLinks())),
                      profile.actualLinks.map: link =>
                        a(href := link.url, targetBlank, noFollow, relMe)(link.site.name)
                    )
                  ),
                  div(cls := "teams col2")(
                    info.teamIds.nonEmpty.option(strong(trans.team.teams())),
                    info.teamIds
                      .sorted(stringOrdering)
                      .map: t =>
                        teamLink(t, withIcon = false)
                  )
                )
              ),
              info.insightVisible.option(
                a(cls := "insight", href := routes.Insight.index(u.username), dataIcon := Icon.Target):
                  span(
                    strong("Chess Insights"),
                    em("Analytics from ", if ctx.is(u) then "your" else s"${u.username}'s", " games")
                  )
              )
            )
          )
      ,
      (ctx.kid.no && info.ublog.so(_.latests).nonEmpty).option(
        div(cls := "user-show__blog ublog-post-cards")(
          info.ublog.so(_.latests).map { views.ublog.postUi.card(_) }
        )
      ),
      div(cls := "angles number-menu number-menu--tabs menu-box-pop")(
        a(
          dataTab := "activity",
          cls := List(
            "nm-item to-activity" -> true,
            "active"              -> (angle == UserInfo.Angle.Activity)
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
          trans.site.nbGames.plural(info.user.count.game, info.user.count.game.localize),
          (info.nbs.playing > 0).option(
            span(
              cls   := "unread",
              title := trans.site.nbPlaying.pluralTxt(info.nbs.playing, info.nbs.playing.localize)
            )(info.nbs.playing)
          )
        )
      )
    )
