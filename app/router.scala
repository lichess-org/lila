package router

import lila.app._
import lila.rating.Perf
import lila.puzzle.PuzzleTheme

// These are only meant for the play router,
// so that controllers can take richer types than routes allow
inline given gameId: Conversion[String, GameId]             = lila.game.Game.strToId(_)
inline given gameFull: Conversion[String, GameFullId]       = GameFullId(_)
inline given gameAny: Conversion[String, GameAnyId]         = GameAnyId(_)
inline given Conversion[String, StudyId]                    = StudyId(_)
inline given Conversion[String, StudyChapterId]             = StudyChapterId(_)
inline given Conversion[String, PuzzleId]                   = PuzzleId(_)
inline given Conversion[String, SimulId]                    = SimulId(_)
inline given Conversion[String, SwissId]                    = SwissId(_)
inline given Conversion[String, TourId]                     = TourId(_)
inline given Conversion[String, TeamId]                     = TeamId(_)
inline given Conversion[String, RelayRoundId]               = RelayRoundId(_)
inline given Conversion[String, UblogPostId]                = UblogPostId(_)
inline given Conversion[String, ForumTopicId]               = ForumTopicId(_)
inline given Conversion[String, ForumPostId]                = ForumPostId(_)
inline given Conversion[String, UserStr]                    = UserStr(_)
inline given Conversion[Option[String], Option[UserStr]]    = UserStr from _
//inline given Conversion[String, lila.forum.ForumPost.Id]    = lila.forum.ForumPost.Id(_)
inline given perfKey: Conversion[String, Perf.Key]          = Perf.Key(_)
inline given puzzleKey: Conversion[String, PuzzleTheme.Key] = PuzzleTheme.Key(_)

// Used when constructing URLs from routes
// TODO actually use the types in the routes
object ReverseRouterConversions:
  inline given Conversion[GameId, String]                   = _.value
  inline given Conversion[GameFullId, String]               = _.value
  inline given Conversion[GameAnyId, String]                = _.value
  inline given Conversion[StudyId, String]                  = _.value
  inline given Conversion[StudyChapterId, String]           = _.value
  inline given Conversion[PuzzleId, String]                 = _.value
  inline given Conversion[SimulId, String]                  = _.value
  inline given Conversion[SwissId, String]                  = _.value
  inline given Conversion[TourId, String]                   = _.value
  inline given Conversion[TeamId, String]                   = _.value
  inline given Conversion[RelayRoundId, String]             = _.value
  inline given Conversion[UblogPostId, String]              = _.value
  inline given Conversion[UserId, String]                   = _.value
  inline given Conversion[UserName, String]                 = _.value
  inline given Conversion[chess.opening.OpeningKey, String] = _.value
  inline given Conversion[chess.format.Uci, String]         = _.uci
  inline given Conversion[Option[UserName], Option[String]] = UserName.raw(_)
  // where a UserStr is accepted, we can pass a UserName or UserId
  inline given Conversion[UserName, UserStr]                       = _ into UserStr
  inline given Conversion[UserId, UserStr]                         = _ into UserStr
  inline given Conversion[ForumTopicId, String]                    = _.value
  inline given postId: Conversion[ForumPostId, String]             = _.value
//  inline given postId: Conversion[lila.forum.ForumPost.Id, String] = _.value
  inline given perfKey: Conversion[Perf.Key, String]               = _.value
  inline given puzzleKey: Conversion[PuzzleTheme.Key, String]      = _.value
