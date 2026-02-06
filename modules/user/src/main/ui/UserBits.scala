package lila.user
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }
import lila.core.relation.Relation
import lila.rating.PerfType

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

  def trophyMeta(perf: PerfType, rank: Int)(using Translate) =
    rank match
      case 1 => Some(("trophy perf top1", s"${perf.trans} Champion!", "images/trophy/gold-cup-2.png"))
      case r if r <= 10 =>
        Some(("trophy perf top10", s"${perf.trans} Top 10!", "images/trophy/silver-cup-2.png"))
      case r if r <= 50 =>
        Some(("trophy perf top50", s"${perf.trans} Top 50 player!", "images/trophy/Fancy-Gold.png"))
      case r if r <= 100 =>
        Some(("trophy perf top100", s"${perf.trans} Top 100 player!", "images/trophy/Gold-Cup.png"))
      case _ => None

  def perfTrophies(u: User, rankMap: lila.core.rating.UserRankMap)(using Translate) = u.lame.not.so:
    rankMap.toList
      .sortBy(_._2)
      .map: (perf, rank) =>
        lila.rating.PerfType(perf) -> rank
      .flatMap: (perf, rank) =>
        trophyMeta(perf, rank).map(perf -> _)
      .map { case (perf, (cssClass, trophyTitle, imgPath)) =>
        a(href := routes.User.top(perf.key))(
          span(cls := cssClass, title := trophyTitle)(
            img(src := assetUrl(imgPath))
          )
        )
      }

  object awards:
    def awardCls(t: Trophy) = cls := s"trophy award ${t.kind._id} ${~t.kind.klass}"

    def maybeLink(urlOpt: Option[String], attrs: Modifier*)(content: Modifier*) =
      urlOpt.filter(_.nonEmpty) match
        case Some(url) => frag(a((attrs :+ (href := url))*)(content*))
        case None      => frag(div(attrs*)(content*))

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
      maybeLink(t.anyUrl, awardCls(t), ariaTitle(t.kind.name))(
        img(src := assetUrl("images/trophy/zug-trophy.png"))
      )
    )
