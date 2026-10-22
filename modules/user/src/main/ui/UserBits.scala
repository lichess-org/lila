package lila.user
package ui

import lila.core.relation.Relation
import lila.rating.PerfType
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class UserBits(helpers: Helpers):
  import helpers.*

  def communityMenu(active: String)(using Translate) =
    lila.ui.bits.pageMenuSubnav(
      a(cls := active.active("leaderboard"), href := routes.User.list)(trans.site.leaderboard()),
      a(
        cls := active.active("ratings"),
        href := routes.User.ratingDistribution(PerfKey.blitz)
      )(
        trans.site.ratingStats()
      ),
      a(cls := active.active("tournament"), href := routes.Tournament.leaderboard)(
        trans.arena.tournamentWinners()
      ),
      a(cls := active.active("shield"), href := routes.Tournament.shields)(
        trans.arena.tournamentShields()
      ),
      div(cls := "sep"),
      a(cls := active.active("bots"), href := routes.PlayApi.botOnline)(
        trans.site.onlineBots()
      ),
      div(cls := "sep"),
      a(cls := active.active("fide"), href := addQueryParam(routes.Fide.index().url, "community", "1"))(
        trans.broadcast.fidePlayers()
      )
    )

  def miniClosed(u: User, relation: Option[Relation])(using Translate) = Snippet:
    frag(
      div(cls := "title")(userLink(u, withPowerTip = false)),
      div(style := "padding: 20px 8px; text-align: center")(trans.settings.thisAccountIsClosed()),
      relation
        .exists(!_.isFollow)
        .option(
          a(
            cls := "btn-rack__btn relation-button text aclose",
            title := trans.site.unblock.txt(),
            href := s"${routes.Relation.unblock(u.id)}?mini=1",
            dataIcon := Icon.NotAllowed
          )(trans.site.blocked())
        ),
      relation
        .exists(_.isFollow)
        .option(
          a(
            cls := "btn-rack__btn relation-button text aclose",
            title := trans.site.unfollow.txt(),
            href := s"${routes.Relation.unfollow(u.id)}?mini=1",
            dataIcon := Icon.ThumbsUp
          )(trans.site.following())
        )
    )

  def signalBars(v: Int) = raw:
    val bars = (1 to 4)
      .map: b =>
        s"""<i${if v < b then " class=\"off\"" else ""}></i>"""
      .mkString("")
    val title = v match
      case 1 => "Poor connection"
      case 2 => "Decent connection"
      case 3 => "Good connection"
      case _ => "Excellent connection"
    s"""<signal title="$title" class="q$v">$bars</signal>"""

  private def trophyMeta(perf: PerfType, rank: Int)(using Translate) =
    rank match
      case 1 => Some(("trophy top1", s"${perf.trans} Champion!", "CHAMPION"))
      case r if r <= 10 => Some(("trophy top10", s"${perf.trans} Top 10!", "TOP 10"))
      case r if r <= 50 => Some(("trophy top50", s"${perf.trans} Top 50 player!", "TOP 50"))
      case r if r <= 100 => Some(("trophy top100", s"${perf.trans} Top 100 player!", "TOP 100"))
      case _ => None

  private val perfBadgePath =
    "M106 16q14-8 28 0l69 40q14 8 14 24v80q0 16-14 24l-69 40q-14 8-28 0l-69-40q-14-8-14-24v-80q0-16 14-24z"

  private val perfBadgeSvg = raw(
    s"""<svg class="badge-svg" viewBox="-20 -20 280 280" aria-hidden="true"><rect class="ribbon-back" x="5" y="94" width="230" height="57" rx="3"/><path class="badge-border" d="$perfBadgePath"/><path class="badge-body" d="$perfBadgePath"/><rect class="ribbon-front" x="7" y="91" width="226" height="58" rx="3"/></svg>"""
  )

  def perfBadge(perf: PerfType, rank: Int, extraCls: String = "")(using Translate): Option[Tag] =
    trophyMeta(perf, rank).map: (cssClass, trophyTitle, label) =>
      span(
        cls := s"$cssClass perf-badge $extraCls".trim,
        title := trophyTitle,
        dataIcon := perf.icon,
        dataText := label
      ):
        perfBadgeSvg

  def perfTrophies(u: User, rankMap: lila.core.rating.UserRankMap)(using Translate) = u.lame.not.so:
    rankMap.toList
      .sortBy(_._2)
      .map: (perf, rank) =>
        lila.rating.PerfType(perf) -> rank
      .flatMap: (perf, rank) =>
        perfBadge(perf, rank).map(a(href := routes.User.top(perf.key))(_))

  object awards:
    def awardCls(t: Trophy) = cls := s"trophy award ${t.kind._id} ${~t.kind.klass}"

    def maybeLink(urlOpt: Option[String]): Tag =
      urlOpt.filter(_.nonEmpty).fold(span)(url => a(href := url))

    def zugMiracleTrophy(t: Trophy) = frag(
      styleTag("""
  .trophy.zugMiracle {
    display: flex;
    align-items: flex-end;
    height: 40px;
    margin: 0 8px!important;
    transition: 2s;
  }
  .trophy.zugMiracle img { height: 60px; }
  @keyframes psyche { 100% { filter: hue-rotate(360deg); } }
  .trophy.zugMiracle:hover {
    transform: translateY(-9px);
    animation: psyche 0.3s ease-in-out infinite alternate;
  }"""),
      maybeLink(t.anyUrl)(awardCls(t), ariaTitle(t.kind.name)):
        img(src := assetUrl("images/trophy/zug-trophy.png"))
    )
