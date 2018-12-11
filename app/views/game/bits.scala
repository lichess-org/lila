package views.html.game

import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.game.{ Game, Pov, Player }
import lila.app.ui.ScalatagsTemplate._
import lila.user.Title

import controllers.routes

object bits {

  def featuredJs(pov: Pov) = Html {
    s"""${gameFenNoCtx(pov, tv = true)}${vstext(pov)(none)}"""
  }

  def mini(pov: Pov)(implicit ctx: Context) = Html {
    s"""${gameFen(pov)}${vstext(pov)(ctx.some)}"""
  }

  def miniBoard(fen: chess.format.FEN, color: chess.Color = chess.White) = Html {
    s"""<div class="mini_board parse_fen is2d" data-color="${color.name}" data-fen="$fen">$miniBoardContent</div>"""
  }

  def watchers(implicit ctx: Context) = Html {
    div(cls := "watchers hidden")(
      span(cls := "number")(nbsp),
      " ",
      trans.spectators.txt().replace(":", ""),
      " ",
      span(cls := "list inline_userlist")
    ).render
  }

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
    initialFen: Option[chess.format.FEN],
    tour: Option[lila.tournament.Tournament],
    cross: Option[lila.game.Crosstable.WithMatchup],
    simul: Option[lila.simul.Simul],
    userTv: Option[lila.user.User] = None,
    bookmarked: Boolean
  )(implicit ctx: Context) = div(cls := "sides")(
    side(pov, initialFen, tour, simul, userTv, bookmarked = bookmarked),
    cross.map { c =>
      div(cls := "crosstable")(crosstable(ctx.userId.fold(c)(c.fromPov), pov.gameId.some))
    }
  )

  def variantLink(
    variant: chess.variant.Variant,
    name: String,
    hintAsTitle: Boolean = false,
    cssClass: String = "hint--bottom",
    initialFen: Option[chess.format.FEN] = None
  ) = a(
    cls := s"$cssClass variant-link",
    href := (variant match {
      case chess.variant.Standard => "https://en.wikipedia.org/wiki/Chess"
      case chess.variant.FromPosition => s"""${routes.Editor.index}?fen=${initialFen.??(_.value.replace(' ', '_'))}"""
      case v => routes.Page.variant(v.key).url
    }),
    rel := "nofollow",
    target := "_blank",
    title := hintAsTitle option variant.title,
    dataHint := !hintAsTitle option variant.title
  )(name)

  private def playerTitle(player: Player) =
    lightUser(player.userId).flatMap(_.title) map Title.apply map { t =>
      span(cls := "title", dataBot(t), title := Title titleName t)(t.value)
    }

  def vstext(pov: Pov)(ctxOption: Option[Context]) =
    div(cls := "vstext clearfix")(
      div(cls := "left user_link")(
        playerUsername(pov.player, withRating = false, withTitle = false),
        br,
        playerTitle(pov.player) map { t => frag(t, " ") },
        pov.player.rating,
        pov.player.provisional option "?"
      ),
      div(cls := "right user_link")(
        playerUsername(pov.opponent, withRating = false, withTitle = false),
        br,
        pov.opponent.rating,
        pov.opponent.provisional option "?",
        playerTitle(pov.opponent) map { t => frag(" ", t) }
      ),
      pov.game.clock map { c =>
        div(cls := "center")(span(cls := "text", dataIcon := "p")(shortClockName(c.config)))
      } orElse {
        ctxOption flatMap { implicit ctx =>
          pov.game.daysPerTurn map { days =>
            div(cls := "center")(
              span(cls := "hint--top", dataHint := trans.correspondence.txt())(
                if (days == 1) trans.oneDay() else trans.nbDays.pluralSame(days)
              )
            )
          }
        }
      }
    )
}
