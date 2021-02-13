package views.html.game

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.{ Game, Pov }

import controllers.routes

object bits {

  def gameIcon(game: Game): Char =
    game.perfType match {
      case _ if game.fromPosition         => '*'
      case _ if game.imported             => '/'
      case Some(p) if game.variant.exotic => p.iconChar
      case _ if game.hasAi                => 'n'
      case Some(p)                        => p.iconChar
      case _                              => '8'
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
      name: String,
      initialFen: Option[chess.format.FEN] = None
  ) =
    a(
      cls := "variant-link",
      href := (variant match {
        case chess.variant.Standard => "https://en.wikipedia.org/wiki/Chess"
        case chess.variant.FromPosition =>
          s"""${routes.Editor.index}?fen=${initialFen.??(_.value.replace(' ', '_'))}"""
        case v => routes.Page.variant(v.key).url
      }),
      rel := "nofollow",
      targetBlank,
      title := variant.title
    )(name)
}
