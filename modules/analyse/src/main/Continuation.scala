package lila.analyse

import chess.Replay

private[analyse] object Continuation {

  def apply(replay: Replay, analysis: Analysis) = {

    val onlyMeaningfulLines = analysis.copy(infos = {
      val plySet = (analysis.infoAdvices collect {
        case (info, Some(_)) ⇒ info.ply
      }).toSet
      analysis.infos map { info ⇒ plySet(info.ply).fold(info, info.dropLine) }
    })

    val convertedToPgn = onlyMeaningfulLines

    convertedToPgn
  }
}
