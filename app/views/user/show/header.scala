package views.user
package show

import lila.app.UiEnv.{ *, given }
import lila.app.mashup.UserInfo
import lila.user.Plan.sinceDate
import lila.user.PlayTime.*
import lila.user.Profile.*

object header:

  private val dataToints = attr("data-toints")
  private val dataTab = attr("data-tab")

  private def possibleSeoBot(u: User) =
    !u.isVerified && !u.hasTitle && u.count.game < 5 && (
      u.profile.exists(_.links.isDefined) ||
        u.profile.flatMap(_.nonEmptyBio).exists(_.contains("https://"))
    )

  def apply(u: User, info: UserInfo, angle: UserInfo.Angle, social: UserInfo.Social)(using ctx: Context) =
    val showLinks = !possibleSeoBot(u) || isGranted(_.Shadowban)
    frag(
      div(cls := "box__top user-show__header")(
        if u.isPatron then
          h1(cls := s"user-link ${if isOnline.exec(u.id) then "online" else "offline"}")(
            a(href := routes.Plan.index())(patronIcon),
            ui.userDom(u)
          )
        else h1(ui.userDom(u)),
        div(
          cls := List(
            "trophies" -> true,
            "packed" -> (info.trophies.countTrophiesAndPerfCups > 7)
          )
        )(
          views.user.bits.perfTrophies(u, info.ranks),
          otherTrophies(info),
          u.plan.active.option(
            a(
              href := routes.Plan.index(),
              cls := "trophy award patron icon3d",
              ariaTitle(trans.patron.patronSince.txt(showDate(u.plan.sinceDate)))
            )(patronIconChar)
          )
        ),
        u.enabled.no.option(span(cls := "closed")("CLOSED"))
      ),
      div(cls := "user-show__social")(
        div(cls := "number-menu")(
          u.noBot.option(
            a(
              href := routes.UserTournament.path(u.username, "recent"),
              cls := "nm-item",
              dataToints := u.toints
            )(
              splitNumber(trans.site.nbTournamentPoints.pluralSame(u.toints))
            )
          ),
          (info.nbSimuls > 0).option(
            a(
              href := routes.Simul.byUser(u.username),
              cls := "nm-item"
            )(
              splitNumber(trans.site.nbSimuls.pluralSame(info.nbSimuls))
            )
          ),
          (info.nbRelays > 0).option(
            a(
              href := routes.RelayTour.by(u.username),
              cls := "nm-item"
            )(
              splitNumber(trans.broadcast.nbBroadcasts.pluralSame(info.nbRelays))
            )
          ),
          a(href := routes.Study.byOwnerDefault(u.username), cls := "nm-item")(
            splitNumber(trans.site.`nbStudies`.pluralSame(info.nbStudies))
          ),
          ctx.kid.no.option(
            a(
              cls := "nm-item",
              href := routes.ForumPost.search("user:" + u.username, 1).url
            )(
              splitNumber(trans.site.nbForumPosts.pluralSame(info.nbForumPosts))
            )
          ),
          (ctx.kid.no && (info.ublog.exists(_.nbPosts > 0) || ctx.is(u))).option(
            a(
              cls := "nm-item",
              href := routes.Ublog.index(u.username)
            )(
              splitNumber(trans.ublog.blogPosts.pluralSame(info.ublog.so(_.nbPosts)))
            )
          ),
          (ctx.isAuth && ctx.isnt(u))
            .option(a(cls := "nm-item note-zone-toggle")(splitNumber(s"${social.notes.size} Notes")))
        ),
        div(cls := "user-actions")(
          isGranted(_.UserModView).option(
            a(
              cls := "mod-zone-toggle",
              href := routes.User.mod(u.username),
              dataIcon := Icon.Agent,
              title := "Mod zone (Hotkey: m)"
            )
          ),
          div(cls := "dropdown")(
            a(dataIcon := Icon.Hamburger),
            div(cls := "dropdown-window")(
              ctx
                .is(u)
                .option(
                  frag(
                    a(
                      cls := "text",
                      href := routes.Account.profile,
                      dataIcon := Icon.Gear
                    )(trans.site.editProfile.txt()),
                    a(
                      cls := "text",
                      href := routes.Relation.blocks(),
                      dataIcon := Icon.NotAllowed
                    )(trans.site.listBlockedPlayers.txt())
                  )
                ),
              a(
                cls := "text",
                href := routes.User.tv(u.username),
                dataIcon := Icon.AnalogTv
              )(trans.site.watchGames.txt()),
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
                cls := "text",
                href := s"${routes.UserAnalysis.index}#explorer/${u.username}",
                dataIcon := Icon.Book
              )(trans.site.openingExplorer.txt()),
              a(
                cls := "text",
                href := routes.User.download(u.username),
                dataIcon := Icon.Download
              )(trans.site.exportGames.txt()),
              (ctx.isAuth && ctx.kid.no && ctx.isnt(u)).option(
                a(
                  cls := "text",
                  href := s"${routes.Report.form}?username=${u.username}",
                  dataIcon := Icon.CautionTriangle
                )(trans.site.reportXToModerators.txt(u.username))
              ),
              (ctx.is(u) || isGranted(_.CloseAccount)).option(
                a(href := routes.Relation.following(u.username), dataIcon := Icon.User)(trans.site.friends())
              ),
              (ctx.is(u) || isGranted(_.BoostHunter)).option(
                a(href := s"${routes.User.opponents}?u=${u.username}", dataIcon := Icon.User)(
                  trans.site.favoriteOpponents()
                )
              ),
              ctx.me
                .soUse(lila.mod.canImpersonate(u.id))
                .option:
                  postForm(action := routes.Mod.impersonate(u.username.value)):
                    submitButton(cls := "btn-rack__btn")("Impersonate")
            )
          )
        )
      ),
      ctx.isnt(u).option(noteUi.zone(u, social.notes)),
      isGranted(_.UserModView).option(div(cls := "mod-zone mod-zone-full none")),
      standardFlash,
      angle match
        case UserInfo.Angle.Games(Some(searchForm)) => views.gameSearch.user(u, searchForm)
        case _ =>
          val profile = u.profileOrDefault
          val hideTroll = u.marks.troll && ctx.isnt(u)
          div(id := "us_profile")(
            if info.ratingChart.isDefined && (!u.lame || ctx.is(u) || isGranted(_.UserModView)) then
              views.user.perfStat.ratingHistoryContainer
            else (ctx.is(u) && u.count.game < 10).option(ui.newPlayer(u)),
            div(cls := "profile-side")(
              div(cls := "user-infos")(
                (u.lame && ctx.isnt(u)).option:
                  div(cls := "warning tos_warning")(
                    span(dataIcon := Icon.CautionCircle, cls := "is4"),
                    trans.site.thisAccountViolatedTos()
                  )
                ,
                (ctx.kid.no && u.kid.no && !hideTroll)
                  .so(profile.nonEmptyRealName)
                  .map(strong(cls := "name")(_)),
                info.publicFideId.map: id =>
                  p(a(href := routes.Fide.show(id, u.username.value))("FIDE player #" + id)),
                (showLinks && ctx.kid.no && u.kid.no && !hideTroll)
                  .so(profile.nonEmptyBio)
                  .map: bio =>
                    p(cls := "bio")(richText(bio, nl2br = true)),
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
                  (!hideTroll && u.kid.no).option(
                    div(cls := "social_links col2")(
                      showLinks
                        .option(profile.actualLinks)
                        .filter(_.nonEmpty)
                        .map: links =>
                          frag(
                            strong(trans.site.socialMediaLinks()),
                            links.map: link =>
                              a(href := link.url, targetBlank, noFollow, relMe)(link.site.name)
                          )
                    )
                  ),
                  (ctx.is(u) || u.kid.no).option(
                    div(cls := "teams col2")(
                      info.teamIds.nonEmpty.option(strong(trans.team.teams())),
                      info.teamIds
                        .sorted(using stringOrdering)
                        .map: t =>
                          teamLink(t, withIcon = false)
                    )
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
          info.ublog.so(_.latests).map(views.ublog.ui.card(_))
        )
      ),
      div(cls := "angles number-menu number-menu--tabs menu-box-pop")(
        a(
          dataTab := "activity",
          cls := List(
            "nm-item to-activity" -> true,
            "active" -> (angle == UserInfo.Angle.Activity)
          ),
          href := routes.User.show(u.username)
        )(trans.activity.activity()),
        a(
          dataTab := "games",
          cls := List(
            "nm-item to-games" -> true,
            "active" -> (angle.key == "games")
          ),
          href := routes.User.gamesAll(u.username)
        )(
          trans.site.nbGames.plural(info.user.count.game, info.user.count.game.localize),
          (info.nbs.playing > 0).option(
            span(
              cls := "unread",
              title := trans.site.nbPlaying.pluralTxt(info.nbs.playing, info.nbs.playing.localize)
            )(info.nbs.playing)
          )
        )
      )
    )
