package lila.user
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class UserActionMenu(helpers: Helpers):
  import helpers.{ *, given }

  def apply(u: User, relationActions: List[MenuItem], canImpersonate: Boolean)(using ctx: Context) =
    Menu(
      List(
        Granter
          .opt(_.UserModView)
          .option(
            MenuItem(
              "Mod zone",
              Icon.agent,
              routes.User.mod(u.username).url,
              cssClass = Some("mod-zone-toggle")
            )
          ),
        ctx
          .is(u)
          .option(
            MenuItem(trans.site.editProfile.txt(), Icon.gear, routes.Account.profile.url)
          ),
        (u.count.game > 0).option(
          MenuItem(trans.site.watch.txt(), Icon.analogTv, routes.User.tv(u.username).url)
        )
      ).flatten ++
        relationActions ++
        List(
          Some(
            MenuItem(
              trans.site.openingExplorer.txt(),
              Icon.book,
              s"${routes.UserAnalysis.index}#explorer/${u.username}"
            )
          ),
          Some(
            MenuItem(trans.site.exportGames.txt(), Icon.download, routes.User.download(u.username).url)
          ),
          (ctx.isAuth && ctx.kid.no && ctx.isnt(u)).option(
            MenuItem(
              trans.site.reportXToModerators.txt(u.username),
              Icon.cautionTriangle,
              s"${routes.Report.form}?username=${u.username}"
            )
          ),
          (ctx.is(u) || Granter.opt(_.CloseAccount)).option(
            MenuItem(trans.site.friends().render, Icon.user, routes.Relation.following(u.username).url)
          ),
          (ctx.is(u) || Granter.opt(_.BoostHunter)).option(
            MenuItem(
              trans.site.favoriteOpponents().render,
              Icon.user,
              s"${routes.User.opponents}?u=${u.username}"
            )
          ),
          ctx
            .is(u)
            .option(
              MenuItem(trans.site.listBlockedPlayers.txt(), Icon.notAllowed, routes.Relation.blocks().url)
            ),
          ctx.me
            .filter(_.is(u))
            .ifTrue(Granter.opt(_.LichessTeam))
            .map: me =>
              MenuItem("My permissions", Icon.logo, routes.Mod.permissions(me.username).url),
          canImpersonate.option(
            MenuItem(
              "Impersonate",
              Icon.agent,
              routes.Mod.impersonate(u.username.value).url,
              httpMethod = Some(HttpMethod.POST)
            )
          )
        ).flatten,
      trans.site.more.txt()
    )
