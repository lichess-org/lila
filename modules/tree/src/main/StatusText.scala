package lila.tree

import chess.variant.Variant
import chess.{ Color, Status }

object StatusText:

  import Status.*

  def apply(status: Status, win: Option[Color], variant: Variant): String =
    status match
      case Aborted => "Game was aborted."
      case Mate => s"${winner(win)} wins by checkmate."
      case Resign => s"${loser(win)} resigns."
      case UnknownFinish => s"${winner(win)} wins."
      case Stalemate => "Draw by stalemate."
      case Timeout if win.isDefined => s"${loser(win)} left the game."
      case Timeout | Draw => "The game is a draw."
      case Outoftime =>
        win match
          case Some(value) => s"${value} wins on time."
          case None => "Draw by time and insufficient material."
      case NoStart => s"${winner(win)} wins by forfeit."
      case Cheat => "Cheat detected."
      case VariantEnd =>
        variant match
          case chess.variant.KingOfTheHill => s"${winner(win)} brings the king to the center."
          case chess.variant.ThreeCheck => s"${winner(win)} gives the third check."
          case chess.variant.RacingKings => s"${winner(win)} wins the race."
          case _ => "Game ends by variant rule."
      case _ => ""

  private def winner(win: Option[Color]) = win.so(_.toString)
  private def loser(win: Option[Color]) = winner(win.map(!_))
