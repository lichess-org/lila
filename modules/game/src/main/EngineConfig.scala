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
    val code: String
    val fullName: String
    val name: String
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

    def apply(sfen: Option[Sfen], variant: Variant, level: Option[Int]): Engine =
      if (
        variant.standard && level.fold(true)(_ > 1) && sfen
          .filterNot(_.initialOf(variant))
          .fold(true)(sfen => Handicap.isHandicap(sfen, variant))
      ) YaneuraOu
      else Fairy
  }

  def apply(sfen: Option[Sfen], variant: Variant, level: Int): EngineConfig =
    EngineConfig(
      level = level,
      engine = Engine(sfen, variant, level.some)
    )

}
