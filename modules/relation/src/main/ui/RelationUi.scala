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
        val name = trans.site.follow.txt()
        val isLong = name.sizeIs > 8
        a(
          cls := s"btn-rack__btn relation-button${(!isLong).so(" text")}",
          dataIcon := Icon.ThumbsUp,
          href := s"${routes.Relation.follow(userId)}?mini=1",
          title := isLong.option(name)
        )((!isLong).option(name))
      case Some(Relation.Follow) =>
        a(
          cls := "btn-rack__btn relation-button text",
          title := trans.site.unfollow.txt(),
          href := s"${routes.Relation.unfollow(userId)}?mini=1",
          dataIcon := Icon.ThumbsUp
        )(trans.site.following())
      case Some(Relation.Block) =>
        a(
          cls := "btn-rack__btn relation-button text",
          title := trans.site.unblock.txt(),
          href := s"${routes.Relation.unblock(userId)}?mini=1",
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
    List(
      (ctx.isnt(user) && !blocked && !blocks).option(
        MenuItem(
          trans.challenge.challengeToPlay.txt(),
          Icon.Swords,
          s"${routes.Lobby.home}?user=${user.name}#friend",
          Some("relation")
        )
      ),
      ctx.me
        .filter(user.isnt(_))
        .so: me =>
          List(
            relation.isEmpty.so:
              List(
                (followable && !blocked).option(
                  MenuItem(
                    trans.site.follow.txt(),
                    Icon.ThumbsUp,
                    routes.Relation.follow(user.name).url,
                    Some("relation"),
                    Some("relation-button")
                  )
                ),
                MenuItem(
                  trans.site.block.txt(),
                  Icon.NotAllowed,
                  routes.Relation.block(user.name).url,
                  Some("relation"),
                  Some("relation-button")
                ).some
              ).flatten
            ,
            blocks.option:
              MenuItem(
                trans.site.unblock.txt(),
                Icon.NotAllowed,
                routes.Relation.unblock(user.name).url,
                Some("relation"),
                Some("relation-button")
              )
            ,
            (!blocked && !blocks && !user.isBot).option(
              MenuItem(
                trans.site.composeMessage.txt(),
                Icon.BubbleSpeech,
                routes.Msg.convo(user.name).url,
                Some("relation")
              )
            ),
            (!blocked && !blocks && !user.isPatron).option:
              val url = if me.isPatron then routes.Plan.list else routes.Plan.index()
              MenuItem(
                trans.patron.giftPatronWingsShort.txt(),
                Icon.Wings,
                s"$url?dest=gift&giftUsername=${user.name}",
                Some("relation")
              )
            ,
            relation
              .has(Relation.Follow)
              .option:
                MenuItem(
                  trans.site.unfollow.txt(),
                  Icon.ThumbsUp,
                  routes.Relation.unfollow(user.name).url,
                  Some("relation"),
                  Some("relation-button")
                )
          ).flatten
    ).flatten

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
        table(cls := "slist slist-pad slist-invert"):
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

  private def page(title: String) =
    Page(title)
      .css("bits.relation")
      .js(infiniteScrollEsmInit)
      .wrap: body =>
        main(cls := "box page-small")(body)

  private def pagTable(pager: Paginator[Related[UserWithPerfs]], call: Call)(using ctx: Context) =
    table(cls := "slist slist-pad slist-invert")(
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
