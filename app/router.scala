package router

import lila.app._
import lila.rating.Perf
import lila.puzzle.PuzzleTheme

// These are only meant for the play router,
// so that controllers can take richer types than routes allow
given gameId: Conversion[String, GameId]             = lila.game.Game.strToId(_)
given gameFull: Conversion[String, GameFullId]       = GameFullId(_)
given gameAny: Conversion[String, GameAnyId]         = GameAnyId(_)
given Conversion[String, StudyId]                    = StudyId(_)
given Conversion[String, StudyChapterId]             = StudyChapterId(_)
given Conversion[String, PuzzleId]                   = PuzzleId(_)
given Conversion[String, SimulId]                    = SimulId(_)
given Conversion[String, SwissId]                    = SwissId(_)
given Conversion[String, TourId]                     = TourId(_)
given Conversion[String, TeamId]                     = TeamId(_)
given Conversion[String, RelayRoundId]               = RelayRoundId(_)
given Conversion[String, UblogPostId]                = UblogPostId(_)
given Conversion[String, ForumCategId]               = ForumCategId(_)
given Conversion[String, ForumTopicId]               = ForumTopicId(_)
given Conversion[String, ForumPostId]                = ForumPostId(_)
given Conversion[String, UserStr]                    = UserStr(_)
given Conversion[Option[String], Option[UserStr]]    = UserStr from _
given perfKey: Conversion[String, Perf.Key]          = Perf.Key(_)
given puzzleKey: Conversion[String, PuzzleTheme.Key] = PuzzleTheme.Key(_)

// Used when constructing URLs from routes
// TODO actually use the types in the routes
object ReverseRouterConversions:
  given Conversion[GameId, String]                   = _.value
  given Conversion[GameFullId, String]               = _.value
  given Conversion[GameAnyId, String]                = _.value
  given Conversion[StudyId, String]                  = _.value
  given Conversion[StudyChapterId, String]           = _.value
  given Conversion[PuzzleId, String]                 = _.value
  given Conversion[SimulId, String]                  = _.value
  given Conversion[SwissId, String]                  = _.value
  given Conversion[TourId, String]                   = _.value
  given Conversion[TeamId, String]                   = _.value
  given Conversion[RelayRoundId, String]             = _.value
  given Conversion[UblogPostId, String]              = _.value
  given Conversion[UserId, String]                   = _.value
  given Conversion[UserName, String]                 = _.value
  given Conversion[chess.opening.OpeningKey, String] = _.value
  given Conversion[chess.format.Uci, String]         = _.uci
  given Conversion[Option[UserName], Option[String]] = UserName.raw(_)
  // where a UserStr is accepted, we can pass a UserName or UserId
  given Conversion[UserName, UserStr]                  = _ into UserStr
  given Conversion[UserId, UserStr]                    = _ into UserStr
  given Conversion[ForumCategId, String]               = _.value
  given Conversion[ForumTopicId, String]               = _.value
  given postId: Conversion[ForumPostId, String]        = _.value
  given perfKey: Conversion[Perf.Key, String]          = _.value
  given puzzleKey: Conversion[PuzzleTheme.Key, String] = _.value
