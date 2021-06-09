package views.html.game

import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.{ Game, Pov }
import lila.rating.PerfType.Correspondence

import controllers.routes

object bits {

  def gameIcon(game: Game): Char =
    game.perfType match {
      case _ if game.fromPosition         => ''
      case _ if game.imported             => ''
      case Some(p) if game.variant.exotic => p.iconChar
      case _ if game.hasAi                => ''
      case Some(p)                        => p.iconChar
      case _                              => ''
    }

  def sides(
      pov: Pov,
      initialFen: Option[chess.format.FEN],
      tour: Option[lila.tournament.TourAndTeamVs],
      cross: Option[lila.game.Crosstable.WithMatchup],
      simul: Option[lila.simul.Simul],
      userTv: Option[lila.user.User] = None,
      bookmarked: Boolean
  )(implicit ctx: Context) =
    div(
      side.meta(pov, initialFen, tour, simul, userTv, bookmarked = bookmarked),
      cross.map { c =>
        div(cls := "crosstable")(crosstable(ctx.userId.fold(c)(c.fromPov), pov.gameId.some))
      }
    )

  def variantLink(
      variant: chess.variant.Variant,
      perfType: Option[lila.rating.PerfType] = None,
      initialFen: Option[chess.format.FEN] = None,
      shortName: Boolean = false
  )(implicit lang: Lang): Frag = {
    def link(
        href: String,
        title: String,
        name: String
    ) = a(
      cls := "variant-link",
      st.href := href,
      targetBlank,
      st.title := title
    )(name)

    if (variant.exotic)
      link(
        href = (variant match {
          case chess.variant.FromPosition =>
            s"""${routes.Editor.index}?fen=${initialFen.??(_.value.replace(' ', '_'))}"""
          case v => routes.Page.variant(v.key).url
        }),
        title = variant.title,
        name = (if (shortName && variant == chess.variant.KingOfTheHill) variant.shortName
                else variant.name).toUpperCase
      )
    else
      perfType match {
        case Some(Correspondence) =>
          link(
            href = s"${routes.Main.faq}#correspondence",
            title = Correspondence.desc,
            name = Correspondence.trans
          )
        case Some(pt) => span(title := pt.desc)(pt.trans)
        case _        => variant.name.toUpperCase
      }
  }
}
