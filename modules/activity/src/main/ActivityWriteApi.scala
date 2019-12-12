package lila.activity

import reactivemongo.api.ReadPreference

import lila.db.dsl._
import lila.game.Game
import lila.study.Study
import lila.user.User

final class ActivityWriteApi(
    coll: Coll,
    studyApi: lila.study.StudyApi
) {

  import Activity._
  import BSONHandlers._
  import activities._
  import model._

  def game(game: Game): Funit = game.userIds.flatMap { userId =>
    for {
      pt <- game.perfType
      player <- game playerByUserId userId
    } yield for {
      a <- getOrCreate(userId)
      setGames = !game.isCorrespondence ?? $doc(
        ActivityFields.games -> a.games.orDefault.add(pt, Score.make(game wonBy player.color, RatingProg make player))
      )
      setCorres = game.hasCorrespondenceClock ?? $doc(
        ActivityFields.corres -> a.corres.orDefault.+(GameId(game.id), false, true)
      )
      setters = setGames ++ setCorres
      _ <- (!setters.isEmpty) ?? coll.update($id(a.id), $set(setters), upsert = true).void
    } yield Unit
  }.sequenceFu.void

  def forumPost(post: lila.forum.Post, topic: lila.forum.Topic): Funit = post.userId.filter(User.lichessId !=) ?? { userId =>
    getOrCreate(userId) flatMap { a =>
      coll.update(
        $id(a.id),
        $set(ActivityFields.posts -> (~a.posts + PostId(post.id))),
        upsert = true
      ).void
    }
  }

  def puzzle(res: lila.puzzle.Puzzle.UserResult): Funit =
    getOrCreate(res.userId) flatMap { a =>
      coll.update(
        $id(a.id),
        $set(ActivityFields.puzzles -> {
          ~a.puzzles + Score.make(
            res = res.result.win.some,
            rp = RatingProg(Rating(res.rating._1), Rating(res.rating._2)).some
          )
        }),
        upsert = true
      ).void
    }

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

  def unfollowAll(from: User, following: Set[User.ID]) =
    coll.distinctWithReadPreference[User.ID, Set](
      "f.o.ids",
      regexId(from.id).some,
      ReadPreference.secondaryPreferred
    ) flatMap { extra =>
        val all = following ++ extra
        all.nonEmpty.?? {
          logger.info(s"${from.id} unfollow ${all.size} users")
          all.map { userId =>
            coll.update(
              regexId(userId) ++ $doc("f.i.ids" -> from.id),
              $pull("f.i.ids" -> from.id)
            )
          }.sequenceFu.void
        }
      }

  def study(id: Study.Id) = studyApi byId id flatMap {
    _.filter(_.isPublic) ?? { s =>
      update(s.ownerId) { a =>
        a.copy(studies = Some(~a.studies + s.id)).some
      }
    }
  }

  def team(id: String, userId: User.ID) =
    update(userId) { a =>
      a.copy(teams = Some(~a.teams + id)).some
    }

  def streamStart(userId: User.ID) =
    update(userId) { _.copy(stream = true).some }

  def erase(user: User) = coll.remove(regexId(user.id))

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
