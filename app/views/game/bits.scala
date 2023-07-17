package views.html.game

import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.game.{ Game, Pov }
import lila.rating.PerfType.Correspondence

import controllers.routes

object bits:

  def gameIcon(game: Game): licon.Icon =
    if game.fromPosition then licon.Feather
    else if game.imported then licon.UploadCloud
    else if game.variant.exotic then game.perfType.icon
    else if game.hasAi then licon.Cogs
    else game.perfType.icon

  def sides(
      pov: Pov,
      initialFen: Option[chess.format.Fen.Epd],
      tour: Option[lila.tournament.TourAndTeamVs],
      cross: Option[lila.game.Crosstable.WithMatchup],
      simul: Option[lila.simul.Simul],
      userTv: Option[lila.user.User] = None,
      bookmarked: Boolean
  )(using ctx: Context) =
    div(
      side.meta(pov, initialFen, tour, simul, userTv, bookmarked = bookmarked),
      cross.map: c =>
        div(cls := "crosstable")(crosstable(ctx.userId.fold(c)(c.fromPov), pov.gameId.some))
    )

  def variantLink(
      variant: chess.variant.Variant,
      perfType: lila.rating.PerfType,
      initialFen: Option[chess.format.Fen.Epd] = None,
      shortName: Boolean = false
  )(using Lang): Frag =

    def link(href: String, title: String, name: String) = a(
      cls     := "variant-link",
      st.href := href,
      targetBlank,
      st.title := title
    )(name)

    if variant.exotic then
      link(
        href = variant match
          case chess.variant.FromPosition =>
            s"""${routes.Editor.index}?fen=${initialFen.so(_.value.replace(' ', '_'))}"""
          case v => routes.ContentPage.variant(v.key).url
        ,
        title = variant.title,
        name = (if shortName && variant == chess.variant.KingOfTheHill then variant.shortName
                else variant.name).toUpperCase
      )
    else if perfType == Correspondence then
      link(
        href = s"${routes.Main.faq}#correspondence",
        title = Correspondence.desc,
        name = Correspondence.trans
      )
    else span(title := perfType.desc)(perfType.trans)
