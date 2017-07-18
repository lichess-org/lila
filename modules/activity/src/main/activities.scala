package lila.activity

import ornicar.scalalib.Zero
import lila.rating.PerfType
import lila.study.Study
import model._

object activities {

  case class Games(value: Map[PerfType, Score]) extends AnyVal {
    def add(pt: PerfType, score: Score) = copy(
      value = value + (pt -> value.get(pt).fold(score)(_ + score))
    )
  }
  implicit val GamesZero = Zero.instance(Games(Map.empty))

  case class Posts(posts: Map[Posts.TopicId, List[Posts.PostId]]) {
    def total = posts.foldLeft(0)(_ + _._2.size)
    def +(postId: Posts.PostId, topicId: Posts.TopicId) = Posts {
      posts + (topicId -> (postId :: ~posts.get(topicId)))
    }
  }
  object Posts {
    case class TopicId(value: String) extends AnyVal
    case class PostId(value: String) extends AnyVal
  }
  implicit val PostsZero = Zero.instance(Posts(Map.empty))

  case class CompAnalysis(gameIds: List[GameId]) extends AnyVal {
    def +(gameId: GameId) = CompAnalysis(gameId :: gameIds)
  }
  implicit val CompsZero = Zero.instance(CompAnalysis(Nil))

  case class Puzzles(score: Score) extends AnyVal {
    def +(s: Score) = Puzzles(score + s)
  }
  implicit val PuzzlesZero = Zero.instance(Puzzles(ScoreZero.zero))

  case class Learn(value: Map[Learn.Stage, Int]) {
    def +(stage: Learn.Stage) = copy(
      value = value + (stage -> value.get(stage).fold(1)(1 +))
    )
  }
  object Learn {
    case class Stage(value: String) extends AnyVal
  }
  implicit val LearnZero = Zero.instance(Learn(Map.empty))

  case class Practice(value: Map[Study.Id, Int]) {
    def +(studyId: Study.Id) = copy(
      value = value + (studyId -> value.get(studyId).fold(1)(1 +))
    )
  }
  implicit val PracticeZero = Zero.instance(Practice(Map.empty))

  case class SimulId(value: String) extends AnyVal
  case class Simuls(value: List[SimulId]) extends AnyVal {
    def +(s: SimulId) = copy(value = s :: value)
  }
  implicit val SimulsZero = Zero.instance(Simuls(Nil))
}
