package views.html.game

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.game.{ Game, Pov, Player }
import lidraughts.user.Title

import controllers.routes

object bits {

  private val dataUserId = attr("data-userid")

  def featuredJs(pov: Pov): Frag = frag(
    gameFenNoCtx(pov, tv = true),
    vstext(pov)(none)
  )

  def mini(pov: Pov, withResult: Boolean = false)(implicit ctx: Context): Frag =
    a(href := gameLink(pov))(
      gameFen(pov, withLink = false, withResult = withResult),
      vstext(pov, withResult)(ctx.some)
    )

  def miniBoard(fen: draughts.format.FEN, color: draughts.Color = draughts.White, boardSize: draughts.Board.BoardSize): Frag = div(
    cls := s"mini-board parse-fen cg-wrap is2d is${boardSize.key}",
    dataColor := color.name,
    dataFen := fen.value,
    dataBoard := s"${boardSize.width}x${boardSize.height}"
  )(cgWrapContent)

  def gameIcon(game: Game): Char = game.perfType match {
    case _ if game.fromPosition => '*'
    case _ if game.imported => '/'
    case Some(p) if game.variant.exotic => p.iconChar
    case _ if game.hasAi => 'n'
    case Some(p) => p.iconChar
    case _ => '8'
  }

  def sides(
    pov: Pov,
    initialFen: Option[draughts.format.FEN],
    tour: Option[lidraughts.tournament.Tournament],
    cross: Option[lidraughts.game.Crosstable.WithMatchup],
    simul: Option[lidraughts.simul.Simul],
    userTv: Option[lidraughts.user.User] = None,
    bookmarked: Boolean
  )(implicit ctx: Context) = div(
    side.meta(pov, initialFen, tour, simul, userTv, bookmarked = bookmarked),
    cross.map { c =>
      div(cls := "crosstable")(crosstable(ctx.userId.fold(c)(c.fromPov), pov.gameId.some))
    }
  )

  def variantLink(
    variant: draughts.variant.Variant,
    name: String,
    initialFen: Option[draughts.format.FEN] = None
  ) = a(
    cls := "variant-link",
    href := (variant match {
      case draughts.variant.FromPosition => s"""${routes.Editor.index}?fen=${initialFen.??(_.value.replace(' ', '_'))}"""
      case v => routes.Page.variant(v.key).url
    }),
    rel := "nofollow",
    target := "_blank",
    title := variant.title
  )(name)

  private def playerTitle(player: Player) =
    lightUser(player.userId).flatMap(_.title) map Title.apply map { t =>
      span(cls := "title", dataBot(t), title := Title titleName t)(t.value)
    }

  def vstext(pov: Pov, withResult: Boolean = false)(ctxOption: Option[Context]): Frag =
    span(cls := "vstext")(
      span(cls := "vstext__pl user-link", dataUserId := withResult ?? pov.player.userId)(
        playerUsername(pov.player, withRating = false, withTitle = false),
        br,
        playerTitle(pov.player) map { t => frag(t, " ") },
        pov.player.rating,
        pov.player.provisional option "?"
      ),
      if (withResult && pov.game.finishedOrAborted) {
        span(cls := "vstext__res")(
          draughts.Color.showResult(pov.game.winnerColor, ctxOption.map(_.pref.draughtsResult).getOrElse(lidraughts.pref.Pref.default.draughtsResult))
        ).some
      } else pov.game.clock map { c =>
        span(cls := "vstext__clock")(shortClockName(c.config))
      } orElse {
        ctxOption flatMap { implicit ctx =>
          pov.game.daysPerTurn map { days =>
            span(cls := "vstext__clock")(
              if (days == 1) trans.oneDay() else trans.nbDays.pluralSame(days)
            )
          }
        }
      },
      span(cls := "vstext__op user-link", dataUserId := withResult ?? pov.opponent.userId)(
        playerUsername(pov.opponent, withRating = false, withTitle = false),
        br,
        pov.opponent.rating,
        pov.opponent.provisional option "?",
        playerTitle(pov.opponent) map { t => frag(" ", t) }
      )
    )
}
