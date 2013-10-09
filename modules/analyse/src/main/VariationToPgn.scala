package lila.analyse

import chess.Replay

private[analyse] object VariationToPgn {

  def apply(replay: Replay, analysis: Analysis): Analysis = {

    val plySet = (analysis.infoAdvices collect {
      case (info, Some(_)) ⇒ info.ply
    }).toSet

    val onlyMeaningfulVariations: List[Info] = analysis.infos map { info ⇒
      plySet(info.ply).fold(info, info.dropVariation)
    }

    def toPgn(ply: Int, variation: List[String]): List[String] =
      replay.chronoMoves lift ply ?? { move =>
      }

    val convertedToPgn: List[Info] = onlyMeaningfulVariations map { info ⇒
      info.copy(
        variation = 
          if (info.variation.isEmpty) Nil 
          else toPgn(info.ply, info.variation)
      )
    }

    analysis.copy(infos = convertedToPgn)
  }
}
