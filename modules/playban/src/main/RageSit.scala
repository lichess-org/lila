package lila.playban

import chess.{ Color, Speed }
import scalalib.ThreadLocalRandom

import lila.core.playban.RageSit

object RageSit:

  object extensions:
    extension (a: RageSit)
      inline def counter: Int = a.value
      def isBad = a.value <= -40
      def isVeryBad = a.value <= -80
      def isTerrible = a.value <= -160
      def isLethal = a.value <= -200

  val empty = lila.core.playban.RageSit(0)

  enum Update:
    case Noop
    case Reset
    case Inc(v: Int)

  def imbalanceInc(game: Game, loser: Color) = Update.Inc:
    {
      import chess.variant.*
      (game.chess.position.materialImbalance, game.variant) match
        case (_, Crazyhouse | Horde | Antichess) => 0
        case (a, _) if a >= 4 => 1
        case (a, _) if a <= -4 => -1
        case _ => 0
    } * {
      if loser.white then 1 else -1
    } * {
      if game.speed <= Speed.Bullet then 5
      else if game.speed == Speed.Blitz then 10
      else 15
    }

  def redeem(game: Game) = Update.Inc:
    game.speed match
      case s if s < Speed.Bullet => 0
      case Speed.Bullet => ThreadLocalRandom.nextInt(2)
      case Speed.Blitz => 1
      case _ => 2
