package lila.activity

import lila.analyse.Analysis
import lila.db.dsl._
import lila.game.Game
import lila.user.User
import lila.user.UserRepo.lichessId

final class ActivityWriteApi(coll: Coll) {

  import Activity._
  import BSONHandlers._
  import activities._

  def game(game: Game): Funit = game.userIds.map { userId =>
    update(userId) { ActivityAggregation.game(game, userId) _ }
  }.sequenceFu.void

  def analysis(analysis: Analysis): Funit = analysis.uid.filter(lichessId !=) ?? { userId =>
    update(userId) { ActivityAggregation.analysis(analysis) _ }
  }

  def forumPost(post: lila.forum.Post, topic: lila.forum.Topic): Funit = post.userId.filter(lichessId !=) ?? { userId =>
    update(userId) { ActivityAggregation.forumPost(post, topic) _ }
  }

  def puzzle(res: lila.puzzle.Puzzle.UserResult): Funit =
    update(res.userId) { ActivityAggregation.puzzle(res) _ }

  def learn(userId: User.ID, stage: String) =
    update(userId) { a => a.copy(learn = a.learn + Learn.Stage(stage)).some }

  def practice(prog: lila.practice.PracticeProgress.OnComplete) =
    update(prog.userId) { a => a.copy(practice = a.practice + prog.studyId).some }

  def simul(simul: lila.simul.Simul) =
    simulParticipant(simul, simul.hostId, true) >>
      simul.pairings.map(_.player.user).map { simulParticipant(simul, _, false) }.sequenceFu.void

  private def simulParticipant(simul: lila.simul.Simul, userId: String, host: Boolean) =
    update(userId) { a => a.copy(simuls = a.simuls + SimulId(simul.id)).some }

  private def get(userId: User.ID) = coll.byId[Activity, Id](Id today userId)
  private def getOrCreate(userId: User.ID) = get(userId) map { _ | Activity.make(userId) }
  private def save(activity: Activity) = coll.update($id(activity.id), activity, upsert = true).void
  private def update(userId: User.ID)(f: Activity => Option[Activity]): Funit =
    getOrCreate(userId) flatMap { old =>
      f(old) ?? save
    }
}
