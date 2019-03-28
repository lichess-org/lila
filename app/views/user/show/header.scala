package views.html.user.show

import play.api.data.Form

import lila.api.Context
import lila.app.mashup.UserInfo.Angle
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.user.User

import controllers.routes

object page {

import social._

private val dataToints = attr("data-toints")

def apply(
  u: User, 
  info: lila.app.mashup.UserInfo, 
  angle: lila.app.mashup.UserInfo.Angle, 
  social: lila.app.mashup.UserInfo.Social)(implicit ctx: Context) =
div(cls := "box__top")(
  h1(cls := s"user_link ${if(isOnline(u.id)) "online" else "offline"}")(
    if (u.isPatron) frag(
    a(cls := routes.Plan.index)(raw(patronIcon)),
    userSpan(u, withPowerTip = false, withOnline = false)
  ) else userSpan(u, withPowerTip = false)
),
  div(cls := List(
    "trophies" -> true,
    "packed" -> (info.countTrophiesAndPerfCups > 7)))(
    perfTrophies(u, info.ranks),
    otherTrophies(u, info)
  ),
  u.plan.active option
  a(href := routes.Plan.index, cls := "trophy award patron icon3d", title := s"Patron since ${showDate(u.plan.sinceDate)}")(patronIconChar),
  u.disabled option span(cls := "closed")("CLOSED")
),
div(cls := "social content_box_inter")(
  div(cls := "links")
    a(cls := "intertab", href := routes.Relation.followers(u.username))(
      splitNumber(trans.nbFollowers.pluralSame(info.nbFollowers))
    ),
    info.nbBlockers.map { nb =>
    a(cls := "intertab")(splitNumberUnsafe(nb + " Blockers"))
    },
    u.noBot option a(
href := routes.UserTournament.path(u.username, "recent"), 
cls := "intertab tournament_stats", 
dataToints := u.toints)(
splitNumber(trans.nbTournamentPoints.pluralSame(u.toints))
),
    a(routes.Study.byOwnerDefault(u.username), cls := "intertab")(
      splitNumberUnsafe(info.nbStudies + " studies")
    ),
    a(cls := "intertab", 
      href := ctx.noKid option routes.ForumPost.search("user:" + u.username, 1))(
      splitNumber(trans.nbForumPosts.pluralSame(info.nbPosts))
    ),
    (ctx.isAuth && ctx.noKid && !ctx.is(u)) option
    a(cls := "intertab note_zone_toggle")(splitNumberUnsafe(notes.size + " Notes")),
  div(cls := "user_actions")(
    (ctx is u) option frag(
    a(cls := "button", href := routes.Account.profile, title := trans.editProfile.frag(), dataIcon := "%"),
    a(cls := "button", href := routes.Relation.blocks(), title := trans.listBlockedPlayers.frag(), dataIcon := "k")
    ),
    isGranted(_.UserSpy) option
    a(cls := "button mod_zone_toggle", href := routes.User.mod(u.username), title := "Mod zone", dataIcon := ""),
    a(cls := "button", href := routes.User.tv(u.username), title := trans.watchGames.txt(), dataIcon := "1"),
    (ctx.isAuth && !ctx.is(u)) option
    views.html.relation.actions(u.id, relation = relation, followable = followable, blocked = blocked),
    ctx.noKid option a(
      title := trans.reportXToModerators.txt(u.username), 
      cls := "button", 
      href := s"${routes.Report.form}username=${u.username}",
      dataIcon := "!"),
    (ctx is u) option
    a(
      cls := "button",
      href := routes.Game.exportByUser(u.username), 
      title := trans.exportGames.txt(), dataIcon := "x")
    }
)
  ),
(ctx.noKid && !ctx.is(u)) option div(cls := "note_zone")(
  form(action := s"${routes.User.writeNote(u.username)}?note", method := "post")(
    textarea(name := "text", placeholder := "Write a note about this user only you and your friends can read"),
    button(tpe := "submit", cls := "button")(trans.send.frag()),
    if (isGranted(_.ModNote)) label(style := "margin-left: 1em;")(
      input(tpe := "checkbox", name := "mod", checked := "true", value := "true", style := "vertical-align: middle;"),
      "For moderators only"
    ) else input(tpe := "hidden", name := "mod", value := "false")
  ),
  notes.isEmpty option div("No note yet"),
  notes.map { note =>
  div(
    p(cls := "meta")(
      userIdLink(note.from.some),
      br,
      momentFromNow(note.date)
      (ctx.me.exists(note.isFrom) && !note.mod) option frag(
        br,
        form(routes.User.deleteNote(note._id), method := "post")(
          button(tpe := "submit", cls := "thin confirm button text", style := "float:right", dataIcon := "q")("Delete")
        )
      )
    ),
    p(cls := "text")(richText(note.text))
  )
  }
),
((ctx is u) && u.perfs.bestStandardRating > 2400 && !u.hasTitle && !ctx.pref.hasSeenVerifyTitle) option claimTitle(u),
isGranted(_.UserSpy) option div(cls := "mod_zone none"),
angle match {
case Angle.Games(Some(searchForm)) => search.user(u, searchForm)
case _ => {
<div id="us_profile" cls := "@if(info.insightVisible){ with_insights}">
  @info.ratingChart.ifTrue(!u.lame || ctx.is(u) || isGranted(_.UserSpy)).map { ratingChart =>
  <div cls := "rating_history">
    @spinner
  </div>
  }.getOrElse {
  @if(ctx.is(u)) {
  @newPlayer(u)
  }
  }
  @defining(u.profileOrDefault) { profile =>
  <div cls := "user-infos scroll-shadow-hard">
    @if(!ctx.is(u)) {
    @if(u.engine) {
    <div cls := "warning engine_warning">
      <span dataIcon := "j" cls := "is4"></span>
      @trans.thisPlayerUsesChessComputerAssistance()
    </div>
    }
    @if(u.booster && (u.count.game > 0 || isGranted(_.Hunter))) {
    <div cls := "warning engine_warning">
      <span dataIcon := "j", cls := "is4"></span>
      @trans.thisPlayerArtificiallyIncreasesTheirRating()
      @if(u.count.game == 0) {
      Only visible to mods. A booster mark without any games is a way to
      prevent a player from ever playing (except against boosters/cheaters).
      It's useful against spambots. These marks are not visible to the public.
      }
    </div>
    }
    } else {
    @u.title.flatMap(lila.user.Title.names.get).map { title =>
    <p dataIcon := "E" cls := "honorific title text">@title</p>
    }
    }
    @NotForKids {
    @profile.nonEmptyRealName.map { name =>
    <strong cls := "name">@name</strong>
    }
    @profile.nonEmptyBio.ifTrue(!u.troll || ctx.is(u)).map { bio =>
    <p cls := "bio">@richText(shorten(bio, 400), nl2br=false)</p>
    }
    }
    <div cls := "stats">
      @profile.officialRating.map { r =>
      <div>@r.name.toUpperCase rating: <strong>@r.rating</strong></div>
      }
      @NotForKids {
      @profile.nonEmptyLocation.map { l =>
      <span cls := "location">@l</span>,
      }
      }
      @profile.countryInfo.map { c =>
      <span cls := "country"><img cls := "flag" src="@staticUrl(s"images/flags/${c.code}.png")" /> @c.name</span>
      }
      <p cls := "thin">@trans.memberSince() @showDate(u.createdAt)</p>
      @u.seenAt.map { seen =>
      <p cls := "thin">@trans.lastSeenActive(momentFromNow(seen))</p>
      }
      @info.completionRatePercent.map { c =>
      <p cls := "thin">@trans.gameCompletionRate(s"$c%")</p>
      }
      @if(ctx is u) {
      <a href="@routes.Account.profile" title="@trans.editProfile()">
        @trans.profileCompletion(s"${profile.completionPercent}%")
      </a>
      <br />
      <a href="@routes.User.opponents">@trans.favoriteOpponents()</a>
      }
      @info.playTime.map { playTime =>
      <br />
      <br />
      <p>@trans.tpTimeSpentPlaying(showPeriod(playTime.totalPeriod))</p>
      @playTime.nonEmptyTvPeriod.map { tvPeriod =>
      <p>@trans.tpTimeSpentOnTV(showPeriod(tvPeriod))</p>
      }
      }
      <div cls := "social_links col2">
        @profile.actualLinks.map { link =>
        <a href="@link.url" target="_blank" rel="no-follow">@link.site.name</a>
        }
      </div>
      <div cls := "teams col2">
        @info.teamIds.sorted.map { t =>
        @teamLink(t, withIcon = false)
        }
      </div>
    </div>
  </div>
  }
  @if(info.insightVisible) {
  <a cls := "insight" href="@routes.Insight.index(u.username)">
    <span cls := "icon" dataIcon := "7"></span>
    <strong>Chess Insights</strong>
    <em>Analytics from @if(ctx.is(u)){your}else{@u.username's} games</em>
  </a>
  }
</div>
}
}
<div cls := "content_box_inter angles tabs">
  <a data-tab="activity" cls := "intertab to_activity @if(angle == Angle.Activity){ active}" href="@routes.User.show(u.username)">@trans.activity.activity()</a>
  @defining(lila.app.mashup.GameFilter.All) { f =>
  <a data-tab="games" cls := "intertab to_games @if(angle.key == "games"){ active}" href="@routes.User.gamesAll(u.username)">
    @trans.nbGames.plural(info.user.count.game, info.user.count.game.localize)
    @if(info.nbs.playing > 0) {
    <span cls := "unread" title="@trans.nbPlaying.plural(info.nbs.playing, info.nbs.playing.localize)">@info.nbs.playing</span>
    }
  </a>
  <span data-tab="other" cls := "intertab@if(angle == Angle.Other){ active}"></span>
  }
</div>
