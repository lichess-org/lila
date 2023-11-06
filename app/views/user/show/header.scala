package views.html.user.show

import controllers.report.routes.{ Report as reportRoutes }
import controllers.routes

import lila.app.mashup.UserInfo
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.richText
import lila.user.User

object header:

  private val dataToints = attr("data-toints")
  private val dataTab    = attr("data-tab")

  def apply(u: User, info: UserInfo, angle: UserInfo.Angle, social: UserInfo.Social)(using ctx: PageContext) =
    frag(
      div(cls := "box__top user-show__header")(
        if u.isPatron then
          h1(cls := s"user-link ${if isOnline(u.id) then "online" else "offline"}")(
            a(href := routes.Plan.index)(patronIcon),
            userSpan(u, withPowerTip = false, withOnline = false)
          )
        else h1(userSpan(u, withPowerTip = false)),
        div(
          cls := List(
            "trophies" -> true,
            "packed"   -> (info.trophies.countTrophiesAndPerfCups > 7)
          )
        )(
          views.html.user.bits.perfTrophies(u, info.ranks),
          otherTrophies(info),
          u.plan.active option
            a(
              href := routes.Plan.index,
              cls  := "trophy award patron icon3d",
              ariaTitle(s"Patron since ${showDate(u.plan.sinceDate)}")
            )(patronIconChar)
        ),
        u.enabled.no option span(cls := "closed")("CLOSED")
      ),
      div(cls := "user-show__social")(
        div(cls := "number-menu")(
          u.noBot option a(
            href       := routes.UserTournament.path(u.username, "recent"),
            cls        := "nm-item",
            dataToints := u.toints
          )(
            splitNumber(trans.nbTournamentPoints.pluralSame(u.toints))
          ),
          info.nbSimuls > 0 option a(
            href := routes.Simul.byUser(u.username),
            cls  := "nm-item"
          )(
            splitNumber(trans.nbSimuls.pluralSame(info.nbSimuls))
          ),
          info.nbRelays > 0 option a(
            href := routes.RelayTour.by(u.username),
            cls  := "nm-item"
          )(
            splitNumber(trans.broadcast.nbBroadcasts.pluralSame(info.nbRelays))
          ),
          a(href := routes.Study.byOwnerDefault(u.username), cls := "nm-item")(
            splitNumber(trans.`nbStudies`.pluralSame(info.nbStudies))
          ),
          ctx.kid.no option a(
            cls  := "nm-item",
            href := routes.ForumPost.search("user:" + u.username, 1).url
          )(
            splitNumber(trans.nbForumPosts.pluralSame(info.nbForumPosts))
          ),
          ctx.kid.no && (info.ublog.exists(_.nbPosts > 0) || ctx.is(u)) option a(
            cls  := "nm-item",
            href := routes.Ublog.index(u.username)
          )(
            splitNumber(s"${info.ublog.so(_.nbPosts)} blog posts")
          ),
          (ctx.isAuth && !ctx.is(u)) option
            a(cls := "nm-item note-zone-toggle")(splitNumber(s"${social.notes.size} Notes"))
        ),
        div(cls := "user-actions btn-rack")(
          (ctx is u) option frag(
            a(
              cls  := "btn-rack__btn",
              href := routes.Account.profile,
              titleOrText(trans.editProfile.txt()),
              dataIcon := licon.Gear
            ),
            a(
              cls  := "btn-rack__btn",
              href := routes.Relation.blocks(),
              titleOrText(trans.listBlockedPlayers.txt()),
              dataIcon := licon.NotAllowed
            )
          ),
          isGranted(_.UserModView) option
            a(
              cls  := "btn-rack__btn mod-zone-toggle",
              href := routes.User.mod(u.username),
              titleOrText("Mod zone (Hotkey: m)"),
              dataIcon := licon.Agent
            ),
          a(
            cls  := "btn-rack__btn",
            href := routes.User.tv(u.username),
            titleOrText(trans.watchGames.txt()),
            dataIcon := licon.AnalogTv
          ),
          !ctx.is(u) option views.html.relation.actions(
            u.light,
            relation = social.relation,
            followable = social.followable,
            blocked = social.blocked
          ),
          a(
            cls  := "btn-rack__btn",
            href := s"${routes.UserAnalysis.index}#explorer/${u.username}",
            titleOrText(trans.openingExplorer.txt()),
            dataIcon := licon.Book
          ),
          a(
            cls  := "btn-rack__btn",
            href := routes.User.download(u.username),
            titleOrText(trans.exportGames.txt()),
            dataIcon := licon.Download
          ),
          (ctx.isAuth && ctx.kid.no && !ctx.is(u)) option a(
            titleOrText(trans.reportXToModerators.txt(u.username)),
            cls      := "btn-rack__btn",
            href     := s"${reportRoutes.form}?username=${u.username}",
            dataIcon := licon.CautionTriangle
          )
        )
      ),
      !ctx.is(u) option noteZone(u, social.notes),
      isGranted(_.UserModView) option div(cls := "mod-zone mod-zone-full none"),
      standardFlash,
      angle match
        case UserInfo.Angle.Games(Some(searchForm)) => views.html.search.user(u, searchForm)
        case _ =>
          val profile   = u.profileOrDefault
          val hideTroll = u.marks.troll && !ctx.is(u)
          div(id := "us_profile")(
            if info.ratingChart.isDefined && (!u.lame || ctx.is(u) || isGranted(_.UserModView)) then
              div(cls := "rating-history")(spinner)
            else (ctx.is(u) && u.count.game < 10) option newPlayer(u),
            div(cls := "profile-side")(
              div(cls := "user-infos")(
                !ctx.is(u) option frag(
                  u.lame option div(cls := "warning tos_warning")(
                    span(dataIcon := licon.CautionCircle, cls := "is4"),
                    trans.thisAccountViolatedTos()
                  )
                ),
                ctx.kid.no && !hideTroll && ctx.kid.no option frag(
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
                  profile.nonEmptyLocation.ifTrue(ctx.kid.no && !hideTroll).map { l =>
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
                  ctx is u option
                    a(href := routes.Account.profile, title := trans.editProfile.txt())(
                      trans.profileCompletion(s"${profile.completionPercent}%")
                    ),
                  ctx.is(u) || isGranted(_.CloseAccount) option frag(
                    br,
                    a(href := routes.Relation.following(u.username))(trans.friends())
                  ),
                  ctx.is(u) || isGranted(_.BoostHunter) option frag(
                    br,
                    a(href := s"${routes.User.opponents}?u=${u.username}")(trans.favoriteOpponents())
                  ),
                  u.playTime.map { playTime =>
                    frag(
                      p(trans.tpTimeSpentPlaying(showDuration(playTime.totalDuration))),
                      playTime.nonEmptyTvDuration.map { tvDuration =>
                        p(trans.tpTimeSpentOnTV(showDuration(tvDuration)))
                      }
                    )
                  },
                  !hideTroll option div(cls := "social_links col2")(
                    profile.actualLinks.nonEmpty option strong(trans.socialMediaLinks()),
                    profile.actualLinks.map { link =>
                      a(href := link.url, targetBlank, noFollow)(link.site.name)
                    }
                  ),
                  div(cls := "teams col2")(
                    info.teamIds.nonEmpty option strong(trans.team.teams()),
                    info.teamIds.sorted(stringOrdering).map { t =>
                      teamLink(t, withIcon = false)
                    }
                  )
                )
              ),
              info.insightVisible option
                a(cls := "insight", href := routes.Insight.index(u.username), dataIcon := licon.Target)(
                  span(
                    strong("Chess Insights"),
                    em("Analytics from ", if ctx.is(u) then "your" else s"${u.username}'s", " games")
                  )
                )
            )
          )
      ,
      (ctx.kid.no && info.ublog.so(_.latests).nonEmpty) option div(cls := "user-show__blog ublog-post-cards")(
        info.ublog.so(_.latests) map { views.html.ublog.post.card(_) }
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
          trans.nbGames.plural(info.user.count.game, info.user.count.game.localize),
          info.nbs.playing > 0 option
            span(
              cls   := "unread",
              title := trans.nbPlaying.pluralTxt(info.nbs.playing, info.nbs.playing.localize)
            )(info.nbs.playing)
        )
      )
    )

  def noteZone(u: User, notes: List[lila.user.Note])(using ctx: PageContext) = div(cls := "note-zone")(
    postForm(cls := "note-form", action := routes.User.writeNote(u.username))(
      form3.textarea(lila.user.UserForm.note("text"))(
        placeholder := trans.writeAPrivateNoteAboutThisUser.txt()
      ),
      if isGranted(_.ModNote) then
        div(cls := "mod-note")(
          submitButton(cls := "button", name := "noteType", value := "mod")("Save Mod Note"),
          isGranted(_.Admin) option submitButton(cls := "button", name := "noteType", value := "dox")(
            "Save Dox Note"
          ),
          submitButton(cls := "button", name := "noteType", value := "normal")("Save Regular Note")
        )
      else submitButton(cls := "button", name := "noteType", value := "normal")(trans.save())
    ),
    notes.isEmpty option div(trans.noNoteYet()),
    notes.map: note =>
      div(cls := "note")(
        p(cls := "note__text")(richText(note.text, expandImg = false)),
        (note.mod && isGranted(_.Admin)) option postForm(
          action := routes.User.setDoxNote(note._id, !note.dox)
        )(
          submitButton(
            cls := "button-empty confirm button text"
          )("Toggle Dox")
        ),
        p(cls := "note__meta")(
          userIdLink(note.from.some),
          br,
          note.dox option "dox ",
          if isGranted(_.ModNote) then momentFromNowServer(note.date)
          else momentFromNow(note.date),
          (ctx.me.exists(note.isFrom) && !note.mod) option frag(
            br,
            postForm(action := routes.User.deleteNote(note._id))(
              submitButton(
                cls      := "button-empty button-red confirm button text",
                style    := "float:right",
                dataIcon := licon.Trash
              )(trans.delete())
            )
          )
        )
      )
  )
