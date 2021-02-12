package lila.activity

import ornicar.scalalib.Zero

import lila.rating.PerfType
import lila.study.Study
import lila.user.User
import model._

object activities {

  val maxSubEntries = 15

  case class Games(value: Map[PerfType, Score]) extends AnyVal {
    def add(pt: PerfType, score: Score) =
      copy(
        value = value + (pt -> value.get(pt).fold(score)(_ add score))
      )
    def hasNonCorres = value.exists(_._1 != PerfType.Correspondence)
  }
  implicit val GamesZero = Zero.instance(Games(Map.empty))

  case class Posts(value: List[PostId]) extends AnyVal {
    def +(postId: PostId) = Posts(postId :: value)
  }
  case class PostId(value: String) extends AnyVal
  implicit val PostsZero = Zero.instance(Posts(Nil))

  case class Puzzles(score: Score) {
    def +(s: Score) = Puzzles(score = score add s)
  }
  implicit val PuzzlesZero = Zero.instance(Puzzles(ScoreZero.zero))

  case class Storm(runs: Int, score: Int) {
    def +(s: Int) = Storm(runs = runs + 1, score = score atLeast s)
  }
  implicit val StormZero = Zero.instance(Storm(0, 0))

  case class Learn(value: Map[Learn.Stage, Int]) {
    def +(stage: Learn.Stage) =
      copy(
        value = value + (stage -> value.get(stage).fold(1)(1 +))
      )
  }
  object Learn {
    case class Stage(value: String) extends AnyVal
  }
  implicit val LearnZero = Zero.instance(Learn(Map.empty))

  case class Practice(value: Map[Study.Id, Int]) {
    def +(studyId: Study.Id) =
      copy(
        value = value + (studyId -> value.get(studyId).fold(1)(1 +))
      )
  }
  implicit val PracticeZero = Zero.instance(Practice(Map.empty))

  case class SimulId(value: String) extends AnyVal
  case class Simuls(value: List[SimulId]) extends AnyVal {
    def +(s: SimulId) = copy(value = s :: value)
  }
  implicit val SimulsZero = Zero.instance(Simuls(Nil))

  case class Corres(moves: Int, movesIn: List[GameId], end: List[GameId]) {
    def add(gameId: GameId, moved: Boolean, ended: Boolean) =
      Corres(
        moves = moves + (moved ?? 1),
        movesIn = if (moved) (gameId :: movesIn).distinct.take(maxSubEntries) else movesIn,
        end = if (ended) (gameId :: end).take(maxSubEntries) else end
      )
  }
  implicit val CorresZero = Zero.instance(Corres(0, Nil, Nil))

  case class Patron(months: Int) extends AnyVal
  case class FollowList(ids: List[User.ID], nb: Option[Int]) {
    def actualNb = nb | ids.size
    def +(id: User.ID) =
      if (ids contains id) this
      else {
        val newIds = (id :: ids).distinct
        copy(
          ids = newIds take maxSubEntries,
          nb = nb.map(1 +).orElse(newIds.size > maxSubEntries option newIds.size)
        )
      }
    def isEmpty = ids.isEmpty
  }
  implicit val FollowListZero = Zero.instance(FollowList(Nil, None))
  implicit val FollowsZero    = Zero.instance(Follows(None, None))

  case class Follows(in: Option[FollowList], out: Option[FollowList]) {
    def addIn(id: User.ID)  = copy(in = Some(~in + id))
    def addOut(id: User.ID) = copy(out = Some(~out + id))
    def isEmpty             = in.fold(true)(_.isEmpty) && out.fold(true)(_.isEmpty)
  }

  case class Studies(value: List[Study.Id]) extends AnyVal {
    def +(s: Study.Id) = copy(value = (s :: value) take maxSubEntries)
  }
  implicit val StudiesZero = Zero.instance(Studies(Nil))

  case class Teams(value: List[String]) extends AnyVal {
    def +(s: String) = copy(value = (s :: value).distinct take maxSubEntries)
  }
  implicit val TeamsZero = Zero.instance(Teams(Nil))
}
