package lila.game

import shogi.format.forsyth.Sfen
import shogi.variant.{ Standard, Variant }
import shogi.{ Handicap, Role }

case class EngineConfig(
    level: Int,
    engine: EngineConfig.Engine
)

object EngineConfig {

  sealed trait Engine {
    val name: String
    val fullName: String
    def jpFullName = fullName
    val code: String
  }
  object Engine {

    case object YaneuraOu extends Engine {
      val name                = "yaneuraou"
      val fullName            = "YaneuraOu"
      override def jpFullName = "やねうら王"
      val code                = "yn"
    }
    case object Fairy extends Engine {
      val name     = "fairy"
      val fullName = "Fairy Stockfish"
      val code     = "fs"
    }

    val default   = YaneuraOu
    val all       = List(YaneuraOu, Fairy)
    val allByCode = all.map(e => e.code -> e).toMap

    def getByCode(code: String): Option[Engine] = allByCode.get(code)

    def apply(initialSfen: Option[Sfen], variant: Variant, level: Option[Int]): Engine =
      if (
        variant.standard && level.fold(true)(_ > 1) && initialSfen
          .filterNot(_.initialOf(variant))
          .fold(true)(sf => Handicap.isHandicap(sf, variant) || isStandardMaterial(sf))
      ) YaneuraOu
      else Fairy
  }

  def apply(sfen: Option[Sfen], variant: Variant, level: Int): EngineConfig =
    EngineConfig(
      level = level,
      engine = Engine(sfen, variant, level.some)
    )

  def isStandardMaterial(sfen: Sfen): Boolean =
    sfen.toSituation(Standard).exists { sit =>
      val default = Standard.pieces.values.map(_.role)
      def countHands(r: Role): Int =
        ~Standard.handRoles.find(_ == r).map(hr => sit.hands.count(hr))
      sit.playable(strict = true, withImpasse = true) &&
      Standard.allRoles.filterNot(r => Standard.unpromote(r).isDefined).forall { r =>
        default
          .count(_ == r) >= (sit.board.count(r) + ~Standard.promote(r).map(sit.board.count) + countHands(r))
      }
    }
}
