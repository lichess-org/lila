package lila.relation
package ui

import scalalib.paginator.Paginator

import lila.core.perf.UserWithPerfs
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class RelationUi(helpers: Helpers):
  import helpers.{ *, given }

  def mini(
      userId: UserId,
      blocked: Boolean,
      followable: Boolean,
      relation: Option[Relation] = None
  )(using Context) =
    relation match
      case None if followable && !blocked =>
        val name   = trans.site.follow.txt()
        val isLong = name.sizeIs > 8
        a(
          cls      := s"btn-rack__btn relation-button${(!isLong).so(" text")}",
          dataIcon := Icon.ThumbsUp,
          href     := s"${routes.Relation.follow(userId)}?mini=1",
          title    := isLong.option(name)
        )((!isLong).option(name))
      case Some(Relation.Follow) =>
        a(
          cls      := "btn-rack__btn relation-button text",
          title    := trans.site.unfollow.txt(),
          href     := s"${routes.Relation.unfollow(userId)}?mini=1",
          dataIcon := Icon.ThumbsUp
        )(trans.site.following())
      case Some(Relation.Block) =>
        a(
          cls      := "btn-rack__btn relation-button text",
          title    := trans.site.unblock.txt(),
          href     := s"${routes.Relation.unblock(userId)}?mini=1",
          dataIcon := Icon.NotAllowed
        )(trans.site.blocked())
      case _ => emptyFrag

  def actions(
      user: lila.core.LightUser,
      relation: Option[Relation],
      followable: Boolean,
      blocked: Boolean
  )(using ctx: Context) =
    val blocks = relation.contains(Relation.Block)
    div(cls := "relation-actions")(
      (ctx.isnt(user) && !blocked && !blocks).option(
        a(
          cls      := "text",
          href     := s"${routes.Lobby.home}?user=${user.name}#friend",
          dataIcon := Icon.Swords
        )(trans.challenge.challengeToPlay.txt())
      ),
      ctx.me
        .filter(user.isnt(_))
        .so: me =>
          frag(
            (!blocked && !blocks && !user.isBot).option(
              a(
                cls      := "text",
                href     := routes.Msg.convo(user.name),
                dataIcon := Icon.BubbleSpeech
              )(trans.site.composeMessage.txt())
            ),
            (!blocked && !blocks && !user.isPatron).option:
              val url = if me.isPatron then routes.Plan.list else routes.Plan.index()
              a(
                cls      := "text",
                href     := s"$url?dest=gift&giftUsername=${user.name}",
                dataIcon := Icon.Wings
              )(trans.patron.giftPatronWingsShort.txt())
            ,
            relation match
              case None =>
                frag(
                  (followable && !blocked).option(
                    a(
                      cls      := "text relation-button",
                      href     := routes.Relation.follow(user.name),
                      dataIcon := Icon.ThumbsUp
                    )(trans.site.follow.txt())
                  ),
                  a(
                    cls      := "text relation-button",
                    href     := routes.Relation.block(user.name),
                    dataIcon := Icon.NotAllowed
                  )(trans.site.block.txt())
                )
              case Some(Relation.Follow) =>
                a(
                  cls      := "text relation-button",
                  href     := routes.Relation.unfollow(user.name),
                  dataIcon := Icon.ThumbsUp
                )(trans.site.unfollow.txt())
              case Some(Relation.Block) =>
                a(
                  cls      := "text relation-button",
                  href     := routes.Relation.unblock(user.name),
                  dataIcon := Icon.NotAllowed
                )(trans.site.unblock.txt())
          )
    )

  def friends(u: User, pag: Paginator[Related[UserWithPerfs]])(using Context) =
    page(s"${u.username} • ${trans.site.friends.txt()}"):
      frag(
        boxTop(
          h1(
            a(href := routes.User.show(u.username), dataIcon := Icon.LessThan, cls := "text"),
            trans.site.friends()
          )
        ),
        pagTable(pag, routes.Relation.following(u.username))
      )

  def blocks(u: User, pag: Paginator[Related[UserWithPerfs]])(using Context) =
    page(s"${u.username} • ${trans.site.blocks.pluralSameTxt(pag.nbResults)}"):
      frag(
        boxTop(
          h1(userLink(u, withOnline = false)),
          div(cls := "actions")(trans.site.blocks.pluralSame(pag.nbResults))
        ),
        pagTable(pag, routes.Relation.blocks())
      )

  def opponents(u: User, sugs: List[Related[UserWithPerfs]])(using ctx: Context) =
    page(s"${u.username} • ${trans.site.favoriteOpponents.txt()}"):
      frag(
        boxTop:
          h1(
            a(href := routes.User.show(u.username), dataIcon := Icon.LessThan, cls := "text"),
            trans.site.favoriteOpponents(),
            " (",
            trans.site.nbGames.pluralSame(lila.core.game.favOpponentOverGames),
            ")"
          )
        ,
        table(cls := "slist slist-pad"):
          tbody:
            if sugs.nonEmpty then
              sugs.map: r =>
                tr(
                  td(userLink(r.user)),
                  ctx.pref.showRatings.option(td(showBestPerf(r.user.perfs))),
                  td:
                    r.nbGames.filter(_ > 0).map { nbGames =>
                      a(href := s"${routes.User.games(u.username, "search")}?players.b=${r.user.username}"):
                        trans.site.nbGames.plural(nbGames, nbGames.localize)
                    }
                )
            else tr(td(trans.site.none()))
      )

  private def page(title: String)(using Context) =
    Page(title)
      .css("bits.relation")
      .js(infiniteScrollEsmInit)
      .wrap: body =>
        main(cls := "box page-small")(body)

  private def pagTable(pager: Paginator[Related[UserWithPerfs]], call: Call)(using ctx: Context) =
    table(cls := "slist slist-pad")(
      if pager.nbResults > 0
      then
        tbody(cls := "infinite-scroll")(
          pager.currentPageResults.map: r =>
            tr(cls := "paginated")(
              td(userLink(r.user)),
              ctx.pref.showRatings.option(td(showBestPerf(r.user.perfs))),
              td(trans.site.nbGames.plural(r.user.count.game, r.user.count.game.localize)),
              td(r.user.seenAt.map: seen =>
                trans.site.lastSeenActive(momentFromNow(seen)))
            ),
          pagerNextTable(pager, np => addQueryParam(call.url, "page", np.toString))
        )
      else tbody(tr(td(colspan := 2)(trans.site.none())))
    )
