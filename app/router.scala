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
inline given Conversion[String, TeamId]                     = TeamId(_)
inline given Conversion[String, RelayRoundId]               = RelayRoundId(_)
inline given Conversion[String, UblogPostId]                = UblogPostId(_)
inline given perfKey: Conversion[String, Perf.Key]          = Perf.Key(_)
inline given puzzleKey: Conversion[String, PuzzleTheme.Key] = PuzzleTheme.Key(_)

// Used when constructing URLs from routes
// TODO actually use the types in the routes
object ReverseRouterConversions:
  inline given Conversion[GameId, String]                     = _.value
  inline given Conversion[GameFullId, String]                 = _.value
  inline given Conversion[GameAnyId, String]                  = _.value
  inline given Conversion[StudyId, String]                    = _.value
  inline given Conversion[StudyChapterId, String]             = _.value
  inline given Conversion[PuzzleId, String]                   = _.value
  inline given Conversion[SimulId, String]                    = _.value
  inline given Conversion[SwissId, String]                    = _.value
  inline given Conversion[TeamId, String]                     = _.value
  inline given Conversion[RelayRoundId, String]               = _.value
  inline given Conversion[UblogPostId, String]                = _.value
  inline given perfKey: Conversion[Perf.Key, String]          = _.value
  inline given puzzleKey: Conversion[PuzzleTheme.Key, String] = _.value
