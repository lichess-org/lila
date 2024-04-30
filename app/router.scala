package router

import chess.variant.Variant

import lila.app.*
import lila.appeal.Appeal
import lila.challenge.Challenge
import lila.clas.{ Clas, ClasInvite }
import lila.puzzle.PuzzleTheme
import lila.report.Report
import lila.core.socket.Sri
import lila.core.id.*
import lila.core.perf.PerfKeyStr

// These are only meant for the play router,
// so that controllers can take richer types than routes allow
given gameId: Conversion[String, GameId]                                 = GameId.take(_)
given gameFull: Conversion[String, GameFullId]                           = GameFullId(_)
given gameAny: Conversion[String, GameAnyId]                             = GameAnyId(_)
given Conversion[String, StudyId]                                        = StudyId(_)
given Conversion[String, StudyChapterId]                                 = StudyChapterId(_)
given Conversion[String, lila.study.Order]                               = lila.study.Order(_)
given Conversion[String, PuzzleId]                                       = PuzzleId(_)
given Conversion[String, SimulId]                                        = SimulId(_)
given Conversion[String, SwissId]                                        = SwissId(_)
given Conversion[String, TourId]                                         = TourId(_)
given Conversion[String, TeamId]                                         = TeamId(_)
given Conversion[String, RelayRoundId]                                   = RelayRoundId(_)
given Conversion[String, UblogPostId]                                    = UblogPostId(_)
given Conversion[String, ForumCategId]                                   = ForumCategId(_)
given Conversion[String, ForumTopicId]                                   = ForumTopicId(_)
given Conversion[String, ForumPostId]                                    = ForumPostId(_)
given Conversion[String, lila.cms.CmsPage.Id]                            = lila.cms.CmsPage.Id(_)
given Conversion[String, lila.cms.CmsPage.Key]                           = lila.cms.CmsPage.Key(_)
given Conversion[String, Sri]                                            = Sri(_)
given Conversion[Int, chess.FideId]                                      = chess.FideId(_)
given challengeId: Conversion[String, Challenge.Id]                      = Challenge.Id(_)
given appealId: Conversion[String, Appeal.Id]                            = Appeal.Id(_)
given reportId: Conversion[String, ReportId]                             = ReportId(_)
given clasId: Conversion[String, Clas.Id]                                = Clas.Id(_)
given clasInviteId: Conversion[String, ClasInvite.Id]                    = ClasInvite.Id(_)
given relayTourInviteId: Conversion[String, lila.relay.RelayTourId]      = lila.relay.RelayTourId(_)
given Conversion[String, UserStr]                                        = UserStr(_)
given userOpt: Conversion[Option[String], Option[UserStr]]               = UserStr.from(_)
given perfKeyStr: Conversion[String, PerfKeyStr]                         = PerfKeyStr(_)
given puzzleKey: Conversion[String, PuzzleTheme.Key]                     = PuzzleTheme.Key(_)
given Conversion[String, Variant.LilaKey]                                = Variant.LilaKey(_)
given variantKeyOpt: Conversion[Option[String], Option[Variant.LilaKey]] = Variant.LilaKey.from(_)
given uciOpt: Conversion[Option[String], Option[chess.format.Uci]]       = _.flatMap(chess.format.Uci(_))

// Used when constructing URLs from routes
// TODO actually use the types in the routes
object ReverseRouterConversions:
  given challengeIdConv: Conversion[Challenge.Id, String]      = _.value
  given appealIdConv: Conversion[Appeal.Id, String]            = _.value
  given clasIdConv: Conversion[Clas.Id, String]                = _.value
  given clasInviteIdConv: Conversion[ClasInvite.Id, String]    = _.value
  given localDateConv: Conversion[java.time.LocalDate, String] = _.toString
