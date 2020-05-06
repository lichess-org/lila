package lila.game

import chess.variant.Variant
import chess.{ Color, Status }

object StatusText {

  import Status._

  def apply(status: Status, win: Option[Color], variant: Variant): String =
    status match {
      case Aborted                  => "Game was aborted."
      case Mate                     => s"${winner(win)} wins by checkmate."
      case Resign                   => s"${loser(win)} resigns."
      case UnknownFinish            => s"${winner(win)} wins."
      case Stalemate                => "Draw by stalemate."
      case Timeout if win.isDefined => s"${loser(win)} left the game."
      case Timeout | Draw           => "The game is a draw."
      case Outoftime                => s"${winner(win)} wins on time."
      case NoStart                  => s"${loser(win)} wins by forfeit."
      case Cheat                    => "Cheat detected."
      case VariantEnd =>
        variant match {
          case chess.variant.KingOfTheHill => s"${winner(win)} brings the king in the center."
          case chess.variant.ThreeCheck    => s"${winner(win)} gives the third check."
          case chess.variant.RacingKings   => s"${winner(win)} wins the race."
          case _                           => "Game ends by variant rule."
        }
      case _ => ""
    }

  def apply(game: lila.game.Game): String = apply(game.status, game.winnerColor, game.variant)

  private def winner(win: Option[Color]) = win.??(_.toString)
  private def loser(win: Option[Color])  = winner(win.map(!_))
}
