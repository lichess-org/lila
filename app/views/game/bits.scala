package views.html.game

import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.game.{ Game, Pov }
import lila.rating.PerfType.Correspondence

import controllers.routes

object bits:

  def gameIcon(game: Game): licon.Icon =
    game.perfType match
      case _ if game.fromPosition         => licon.Feather
      case _ if game.imported             => licon.UploadCloud
      case Some(p) if game.variant.exotic => p.icon
      case _ if game.hasAi                => licon.Cogs
      case Some(p)                        => p.icon
      case _                              => licon.Crown

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
      perfType: Option[lila.rating.PerfType] = None,
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
        name = (if (shortName && variant == chess.variant.KingOfTheHill) variant.shortName
                else variant.name).toUpperCase
      )
    else
      perfType match
        case Some(Correspondence) =>
          link(
            href = s"${routes.Main.faq}#correspondence",
            title = Correspondence.desc,
            name = Correspondence.trans
          )
        case Some(pt) => span(title := pt.desc)(pt.trans)
        case _        => variant.name.toUpperCase
