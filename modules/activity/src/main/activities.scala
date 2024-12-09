package lila.activity

import alleycats.Zero

import lila.activity.Score.plus
import lila.core.chess.Rank
import lila.core.rating.Score

object activities:

  val maxSubEntries = 15

  opaque type Games = Map[PerfKey, Score]
  object Games extends TotalWrapper[Games, Map[PerfKey, Score]]:
    extension (a: Games)
      def add(pt: PerfKey, score: Score): Games =
        a.value + (pt -> a.value.get(pt).fold(score)(_.plus(score)))
      def hasNonCorres                          = a.value.exists(_._1 != PerfKey.correspondence)
    given Zero[Games] = Zero(Map.empty)

  opaque type ForumPosts = List[ForumPostId]
  object ForumPosts extends TotalWrapper[ForumPosts, List[ForumPostId]]:
    extension (a: ForumPosts) def +(postId: ForumPostId): ForumPosts = postId :: a.value
    given Zero[ForumPosts]                                           = Zero(Nil)

  opaque type UblogPosts = List[UblogPostId]
  object UblogPosts extends TotalWrapper[UblogPosts, List[UblogPostId]]:
    extension (a: UblogPosts) def +(postId: UblogPostId): UblogPosts = postId :: a.value
    given Zero[UblogPosts]                                           = Zero(Nil)

  opaque type Puzzles = Score
  object Puzzles extends TotalWrapper[Puzzles, Score]:
    extension (a: Puzzles) def +(s: Score) = Puzzles(a.value.plus(s))
    given Zero[Puzzles]                    = Zero(lila.activity.Score.empty)

  case class Storm(runs: Int, score: Int):
    def +(s: Int) = Storm(runs = runs + 1, score = score.atLeast(s))
  object Storm:
    given Zero[Storm] = Zero(Storm(0, 0))

  case class Racer(runs: Int, score: Int):
    def +(s: Int) = Racer(runs = runs + 1, score = score.atLeast(s))
  object Racer:
    given Zero[Racer] = Zero(Racer(0, 0))

  case class Streak(runs: Int, score: Int):
    def +(s: Int) = Streak(runs = runs + 1, score = score.atLeast(s))
  object Streak:
    given Zero[Streak] = Zero(Streak(0, 0))

  opaque type LearnStage = String
  object LearnStage extends OpaqueString[LearnStage]

  opaque type Learn = Map[LearnStage, Int]
  object Learn extends TotalWrapper[Learn, Map[LearnStage, Int]]:
    extension (a: Learn)
      def +(stage: LearnStage): Learn = a.value + (stage -> a.value.get(stage).fold(1)(1 +))
    given Zero[Learn]                 = Zero(Map.empty)

  opaque type Practice = Map[StudyId, Int]
  object Practice extends TotalWrapper[Practice, Map[StudyId, Int]]:
    extension (a: Practice)
      def +(studyId: StudyId): Practice =
        a.value + (studyId -> a.value.get(studyId).fold(1)(1 +))
    given Zero[Practice]                = Zero(Map.empty)

  opaque type Simuls = List[SimulId]
  object Simuls extends TotalWrapper[Simuls, List[SimulId]]:
    extension (a: Simuls) def +(s: SimulId): Simuls = s :: a.value
    given Zero[Simuls]                              = Zero(Nil)

  case class Corres(moves: Int, movesIn: List[GameId], end: List[GameId]):
    def add(gameId: GameId, moved: Boolean, ended: Boolean) =
      Corres(
        moves = moves + (moved.so(1)),
        movesIn = if moved then (gameId :: movesIn).distinct.take(maxSubEntries) else movesIn,
        end = if ended then (gameId :: end).take(maxSubEntries) else end
      )
  object Corres:
    given Zero[Corres] = Zero(Corres(0, Nil, Nil))

  case class Patron(months: Int)
  case class FollowList(ids: List[UserId], nb: Option[Int]):
    def actualNb = nb | ids.size
    def +(id: UserId) =
      if ids contains id then this
      else
        val newIds = (id :: ids).distinct
        copy(
          ids = newIds.take(maxSubEntries),
          nb = nb.map(1 +).orElse((newIds.size > maxSubEntries).option(newIds.size))
        )
    def isEmpty = ids.isEmpty
  given Zero[FollowList] = Zero(FollowList(Nil, None))
  given Zero[Follows]    = Zero(Follows(None, None))

  case class Follows(in: Option[FollowList], out: Option[FollowList]):
    def addIn(id: UserId)  = copy(in = Some(~in + id))
    def addOut(id: UserId) = copy(out = Some(~out + id))
    def isEmpty            = in.forall(_.isEmpty) && out.forall(_.isEmpty)
    def allUserIds         = in.so(_.ids) ::: out.so(_.ids)

  opaque type Studies = List[StudyId]
  object Studies extends TotalWrapper[Studies, List[StudyId]]:
    extension (a: Studies) def +(s: StudyId): Studies = (s :: a.value).take(maxSubEntries)
    given Zero[Studies]                               = Zero(Nil)

  opaque type Teams = List[TeamId]
  object Teams extends TotalWrapper[Teams, List[TeamId]]:
    extension (a: Teams) def +(s: TeamId): Teams = (s :: a.value).distinct.take(maxSubEntries)
    given Zero[Teams]                            = Zero(Nil)

  case class SwissRank(id: SwissId, rank: Rank)

  opaque type Swisses = List[SwissRank]
  object Swisses extends TotalWrapper[Swisses, List[SwissRank]]:
    extension (a: Swisses) def +(s: SwissRank): Swisses = (s :: a.value).take(maxSubEntries)
    given Zero[Swisses]                                 = Zero(Nil)
