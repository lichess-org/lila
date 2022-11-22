package lila.activity

import model.*
import alleycats.Zero

import lila.rating.PerfType
import lila.study.Study
import lila.swiss.Swiss
import lila.user.User

object activities:

  val maxSubEntries = 15

  case class Games(value: Map[PerfType, Score]):
    def add(pt: PerfType, score: Score) =
      copy(
        value = value + (pt -> value.get(pt).fold(score)(_ add score))
      )
    def hasNonCorres = value.exists(_._1 != PerfType.Correspondence)
  given Zero[Games] = Zero(Games(Map.empty))

  case class ForumPosts(value: List[ForumPostId]) extends AnyVal:
    def +(postId: ForumPostId) = ForumPosts(postId :: value)
  case class ForumPostId(value: String) extends AnyVal
  given Zero[ForumPosts] = Zero(ForumPosts(Nil))

  case class UblogPosts(value: List[UblogPostId]) extends AnyVal:
    def +(postId: UblogPostId) = UblogPosts(postId :: value)
  case class UblogPostId(value: String) extends AnyVal
  given Zero[UblogPosts] = Zero(UblogPosts(Nil))

  case class Puzzles(score: Score):
    def +(s: Score) = Puzzles(score = score add s)
  given Zero[Puzzles] = Zero(Puzzles(summon[Zero[Score]].zero))

  case class Storm(runs: Int, score: Int):
    def +(s: Int) = Storm(runs = runs + 1, score = score atLeast s)
  given Zero[Storm] = Zero(Storm(0, 0))

  case class Racer(runs: Int, score: Int):
    def +(s: Int) = Racer(runs = runs + 1, score = score atLeast s)
  given Zero[Racer] = Zero(Racer(0, 0))

  case class Streak(runs: Int, score: Int):
    def +(s: Int) = Streak(runs = runs + 1, score = score atLeast s)
  given Zero[Streak] = Zero(Streak(0, 0))

  case class Learn(value: Map[Learn.Stage, Int]):
    def +(stage: Learn.Stage) =
      copy(
        value = value + (stage -> value.get(stage).fold(1)(1 +))
      )
  object Learn:
    opaque type Stage = String
    object Stage extends OpaqueString[Stage]
  given Zero[Learn] = Zero(Learn(Map.empty))

  case class Practice(value: Map[StudyId, Int]):
    def +(studyId: StudyId) =
      copy(
        value = value + (studyId -> value.get(studyId).fold(1)(1 +))
      )
  given Zero[Practice] = Zero(Practice(Map.empty))

  case class SimulId(value: String) extends AnyVal
  case class Simuls(value: List[SimulId]) extends AnyVal:
    def +(s: SimulId) = copy(value = s :: value)
  given Zero[Simuls] = Zero(Simuls(Nil))

  case class Corres(moves: Int, movesIn: List[GameId], end: List[GameId]):
    def add(gameId: GameId, moved: Boolean, ended: Boolean) =
      Corres(
        moves = moves + (moved ?? 1),
        movesIn = if (moved) (gameId :: movesIn).distinct.take(maxSubEntries) else movesIn,
        end = if (ended) (gameId :: end).take(maxSubEntries) else end
      )
  given Zero[Corres] = Zero(Corres(0, Nil, Nil))

  case class Patron(months: Int)
  case class FollowList(ids: List[User.ID], nb: Option[Int]):
    def actualNb = nb | ids.size
    def +(id: User.ID) =
      if (ids contains id) this
      else
        val newIds = (id :: ids).distinct
        copy(
          ids = newIds take maxSubEntries,
          nb = nb.map(1 +).orElse(newIds.size > maxSubEntries option newIds.size)
        )
    def isEmpty = ids.isEmpty
  given Zero[FollowList] = Zero(FollowList(Nil, None))
  given Zero[Follows]    = Zero(Follows(None, None))

  case class Follows(in: Option[FollowList], out: Option[FollowList]):
    def addIn(id: User.ID)  = copy(in = Some(~in + id))
    def addOut(id: User.ID) = copy(out = Some(~out + id))
    def isEmpty             = in.fold(true)(_.isEmpty) && out.fold(true)(_.isEmpty)
    def allUserIds          = in.??(_.ids) ::: out.??(_.ids)

  case class Studies(value: List[StudyId]) extends AnyVal:
    def +(s: StudyId) = copy(value = (s :: value) take maxSubEntries)
  given Zero[Studies] = Zero(Studies(Nil))

  case class Teams(value: List[String]) extends AnyVal:
    def +(s: String) = copy(value = (s :: value).distinct take maxSubEntries)
  given Zero[Teams] = Zero(Teams(Nil))

  case class SwissRank(id: SwissId, rank: Int)
  case class Swisses(value: List[SwissRank]) extends AnyVal:
    def +(s: SwissRank) = copy(value = (s :: value) take maxSubEntries)
  given Zero[Swisses] = Zero(Swisses(Nil))
