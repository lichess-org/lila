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
  import model._

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
    update(userId) { a => a.copy(learn = Some(~a.learn + Learn.Stage(stage))).some }

  def practice(prog: lila.practice.PracticeProgress.OnComplete) =
    update(prog.userId) { a => a.copy(practice = Some(~a.practice + prog.studyId)).some }

  def simul(simul: lila.simul.Simul) =
    simulParticipant(simul, simul.hostId, true) >>
      simul.pairings.map(_.player.user).map { simulParticipant(simul, _, false) }.sequenceFu.void

  def corresMove(gameId: Game.ID, userId: User.ID) =
    update(userId) { a =>
      a.copy(corres = Some(~a.corres + (GameId(gameId), true, false))).some
    }

  def plan(userId: User.ID, months: Int) =
    update(userId) { a =>
      a.copy(patron = Some(Patron(months))).some
    }

  def follow(from: User.ID, to: User.ID) =
    update(from) { a =>
      a.copy(follows = Some(~a.follows addOut to)).some
    } >>
      update(to) { a =>
        a.copy(follows = Some(~a.follows addIn from)).some
      }

  private def simulParticipant(simul: lila.simul.Simul, userId: String, host: Boolean) =
    update(userId) { a => a.copy(simuls = Some(~a.simuls + SimulId(simul.id))).some }

  private def get(userId: User.ID) = coll.byId[Activity, Id](Id today userId)
  private def getOrCreate(userId: User.ID) = get(userId) map { _ | Activity.make(userId) }
  private def save(activity: Activity) = coll.update($id(activity.id), activity, upsert = true).void
  private def update(userId: User.ID)(f: Activity => Option[Activity]): Funit =
    getOrCreate(userId) flatMap { old =>
      f(old) ?? save
    }
}
