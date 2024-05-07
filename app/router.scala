package router

import lila.app.*
import lila.appeal.Appeal
import lila.challenge.Challenge
import lila.puzzle.PuzzleTheme
import lila.report.Report
import lila.core.socket.Sri
import lila.core.id.*

// These are only meant for the play router,
// so that controllers can take richer types than routes allow
given Conversion[String, StudyId]                                   = StudyId(_)
given Conversion[String, StudyChapterId]                            = StudyChapterId(_)
given Conversion[String, lila.study.Order]                          = lila.study.Order(_)
given Conversion[String, PuzzleId]                                  = PuzzleId(_)
given Conversion[String, SimulId]                                   = SimulId(_)
given Conversion[String, SwissId]                                   = SwissId(_)
given Conversion[String, TourId]                                    = TourId(_)
given Conversion[String, TeamId]                                    = TeamId(_)
given Conversion[String, RelayRoundId]                              = RelayRoundId(_)
given Conversion[String, UblogPostId]                               = UblogPostId(_)
given Conversion[String, ForumCategId]                              = ForumCategId(_)
given Conversion[String, ForumTopicId]                              = ForumTopicId(_)
given Conversion[String, ForumPostId]                               = ForumPostId(_)
given Conversion[String, lila.cms.CmsPage.Id]                       = lila.cms.CmsPage.Id(_)
given Conversion[String, lila.cms.CmsPage.Key]                      = lila.cms.CmsPage.Key(_)
given Conversion[String, Sri]                                       = Sri(_)
given Conversion[Int, chess.FideId]                                 = chess.FideId(_)
given challengeId: Conversion[String, ChallengeId]                  = ChallengeId(_)
given appealId: Conversion[String, AppealId]                        = AppealId(_)
given reportId: Conversion[String, ReportId]                        = ReportId(_)
given relayTourInviteId: Conversion[String, lila.relay.RelayTourId] = lila.relay.RelayTourId(_)
given Conversion[String, UserStr]                                   = UserStr(_)
given userOpt: Conversion[Option[String], Option[UserStr]]          = UserStr.from(_)
given puzzleKey: Conversion[String, PuzzleTheme.Key]                = PuzzleTheme.Key(_)
given uciOpt: Conversion[Option[String], Option[chess.format.Uci]]  = _.flatMap(chess.format.Uci(_))

// Used when constructing URLs from routes
// TODO actually use the types in the routes
object ReverseRouterConversions:
  given appealIdConv: Conversion[AppealId, String]             = _.value
  given localDateConv: Conversion[java.time.LocalDate, String] = _.toString
