package lila.title

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

import chess.PlayerTitle
private val availableTitles = PlayerTitle.acronyms.filter: t =>
  t != PlayerTitle.LM && t != PlayerTitle.BOT
