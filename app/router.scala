package router

import lila.app.*
import lila.puzzle.PuzzleTheme
import lila.report.Report
import lila.core.socket.Sri
import lila.core.id.*

// These are only meant for the play router,
// so that controllers can take richer types than routes allow
given Conversion[String, lila.study.Order]                          = lila.study.Order(_)
given Conversion[String, RelayRoundId]                              = RelayRoundId(_)
given Conversion[String, lila.cms.CmsPage.Id]                       = lila.cms.CmsPage.Id(_)
given Conversion[String, lila.cms.CmsPage.Key]                      = lila.cms.CmsPage.Key(_)
given challengeId: Conversion[String, ChallengeId]                  = ChallengeId(_)
given appealId: Conversion[String, AppealId]                        = AppealId(_)
given reportId: Conversion[String, ReportId]                        = ReportId(_)
given relayTourInviteId: Conversion[String, lila.relay.RelayTourId] = lila.relay.RelayTourId(_)
given userOpt: Conversion[Option[String], Option[UserStr]]          = UserStr.from(_)
given puzzleKey: Conversion[String, PuzzleTheme.Key]                = PuzzleTheme.Key(_)
given uciOpt: Conversion[Option[String], Option[chess.format.Uci]]  = _.flatMap(chess.format.Uci(_))
