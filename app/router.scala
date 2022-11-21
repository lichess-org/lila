package router

import lila.app._

// These are only meant for the play router,
// so that controllers can take richer types than routes allow
given Conversion[String, GameId]         = lila.game.Game.takeGameId
given Conversion[String, GameFullId]     = GameFullId.apply
given Conversion[String, StudyId]        = StudyId.apply
given Conversion[String, StudyChapterId] = StudyChapterId.apply
