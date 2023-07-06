package router

import lila.app.*
import lila.rating.Perf
import lila.puzzle.PuzzleTheme
import lila.report.Report
import lila.appeal.Appeal
import chess.variant.Variant
import lila.clas.{ Clas, ClasInvite }
import lila.challenge.Challenge
import lila.socket.Socket.Sri

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
given Conversion[String, Sri]                                            = Sri(_)
given challengeId: Conversion[String, Challenge.Id]                      = Challenge.Id(_)
given appealId: Conversion[String, Appeal.Id]                            = Appeal.Id(_)
given reportId: Conversion[String, Report.Id]                            = Report.Id(_)
given clasId: Conversion[String, Clas.Id]                                = Clas.Id(_)
given clasInviteId: Conversion[String, ClasInvite.Id]                    = ClasInvite.Id(_)
given relayTourInviteId: Conversion[String, lila.relay.RelayTour.Id]     = lila.relay.RelayTour.Id(_)
given Conversion[String, UserStr]                                        = UserStr(_)
given userOpt: Conversion[Option[String], Option[UserStr]]               = UserStr from _
given perfKey: Conversion[String, Perf.Key]                              = Perf.Key(_)
given puzzleKey: Conversion[String, PuzzleTheme.Key]                     = PuzzleTheme.Key(_)
given Conversion[String, Variant.LilaKey]                                = Variant.LilaKey(_)
given variantKeyOpt: Conversion[Option[String], Option[Variant.LilaKey]] = Variant.LilaKey.from(_)
given uciOpt: Conversion[Option[String], Option[chess.format.Uci]]       = _.flatMap(chess.format.Uci(_))

// Used when constructing URLs from routes
// TODO actually use the types in the routes
object ReverseRouterConversions:
  given Conversion[GameId, String]                                         = _.value
  given Conversion[GameFullId, String]                                     = _.value
  given Conversion[GameAnyId, String]                                      = _.value
  given Conversion[StudyId, String]                                        = _.value
  given Conversion[StudyChapterId, String]                                 = _.value
  given Conversion[PuzzleId, String]                                       = _.value
  given Conversion[SimulId, String]                                        = _.value
  given Conversion[SwissId, String]                                        = _.value
  given Conversion[TourId, String]                                         = _.value
  given Conversion[TeamId, String]                                         = _.value
  given Conversion[RelayRoundId, String]                                   = _.value
  given Conversion[UblogPostId, String]                                    = _.value
  given Conversion[UserId, String]                                         = _.value
  given Conversion[UserName, String]                                       = _.value
  given Conversion[chess.opening.OpeningKey, String]                       = _.value
  given Conversion[chess.format.Uci, String]                               = _.uci
  given Conversion[Variant.LilaKey, String]                                = _.value
  given variantKeyOpt: Conversion[Option[Variant.LilaKey], Option[String]] = Variant.LilaKey.raw(_)
  given Conversion[Option[UserName], Option[String]]                       = UserName.raw(_)
  // where a UserStr is accepted, we can pass a UserName or UserId
  given Conversion[UserName, UserStr]                                = _ into UserStr
  given Conversion[UserId, UserStr]                                  = _ into UserStr
  given Conversion[ForumCategId, String]                             = _.value
  given Conversion[ForumTopicId, String]                             = _.value
  given challengeIdConv: Conversion[Challenge.Id, String]            = _.value
  given appealIdConv: Conversion[Appeal.Id, String]                  = _.value
  given reportIdConv: Conversion[Report.Id, String]                  = _.value
  given postIdConv: Conversion[ForumPostId, String]                  = _.value
  given clasIdConv: Conversion[Clas.Id, String]                      = _.value
  given clasInviteIdConv: Conversion[ClasInvite.Id, String]          = _.value
  given relayTourIdConv: Conversion[lila.relay.RelayTour.Id, String] = _.value
  given perfKeyConv: Conversion[Perf.Key, String]                    = _.value
  given puzzleKeyConv: Conversion[PuzzleTheme.Key, String]           = _.value
