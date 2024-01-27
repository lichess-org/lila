package lila.game

import shogi.format.forsyth.Sfen
import shogi.variant.Variant
import shogi.Handicap

case class EngineConfig(
    level: Int,
    engine: EngineConfig.Engine
)

object EngineConfig {

  sealed trait Engine {
    val name: String
    val fullName: String
    val code: String
  }
  object Engine {

    case object YaneuraOu extends Engine {
      val name     = "yaneuraou"
      val fullName = "YaneuraOu"
      val code     = "yn"
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
          .fold(true)(sf => Handicap.isHandicap(sf, variant))
      ) YaneuraOu
      else Fairy
  }

  def apply(sfen: Option[Sfen], variant: Variant, level: Int): EngineConfig =
    EngineConfig(
      level = level,
      engine = Engine(sfen, variant, level.some)
    )

}
