package lila.activity

import lila.db.dsl._
import lila.game.Game
import lila.study.Study
import lila.user.User

final class ActivityWriteApi(
    coll: Coll,
    studyApi: lila.study.StudyApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Activity._
  import BSONHandlers._
  import activities._
  import model._

  def game(game: Game): Funit =
    game.userIds
      .flatMap { userId =>
        for {
          pt     <- game.perfType
          player <- game playerByUserId userId
        } yield for {
          a <- getOrCreate(userId)
          setGames = !game.isCorrespondence ?? $doc(
            ActivityFields.games -> a.games.orDefault
              .add(pt, Score.make(game wonBy player.color, RatingProg make player))
          )
          setCorres = game.hasCorrespondenceClock ?? $doc(
            ActivityFields.corres -> a.corres.orDefault.add(GameId(game.id), moved = false, ended = true)
          )
          setters = setGames ++ setCorres
          _ <-
            (!setters.isEmpty) ?? coll.update
              .one($id(a.id), $set(setters), upsert = true)
              .void
        } yield ()
      }
      .sequenceFu
      .void

  def forumPost(post: lila.forum.Post): Funit =
    post.userId.filter(User.lichessId !=) ?? { userId =>
      getOrCreate(userId) flatMap { a =>
        coll.update
          .one(
            $id(a.id),
            $set(ActivityFields.forumPosts -> (~a.forumPosts + ForumPostId(post.id))),
            upsert = true
          )
          .void
      }
    }

  def ublogPost(post: lila.ublog.UblogPost): Funit =
    getOrCreate(post.user) flatMap { a =>
      coll.update
        .one(
          $id(a.id),
          $set(ActivityFields.ublogPosts -> (~a.ublogPosts + UblogPostId(post.id.value))),
          upsert = true
        )
        .void
    }

  def puzzle(res: lila.puzzle.Puzzle.UserResult): Funit =
    getOrCreate(res.userId) flatMap { a =>
      coll.update
        .one(
          $id(a.id),
          $set(ActivityFields.puzzles -> {
            ~a.puzzles + Score.make(
              res = res.result.win.some,
              rp = RatingProg(Rating(res.rating._1), Rating(res.rating._2)).some
            )
          }),
          upsert = true
        )
        .void
    }

  def storm(userId: User.ID, score: Int): Funit =
    getOrCreate(userId) flatMap { a =>
      coll.update
        .one(
          $id(a.id),
          $set(ActivityFields.storm -> { ~a.storm + score }),
          upsert = true
        )
        .void
    }

  def racer(userId: User.ID, score: Int): Funit =
    getOrCreate(userId) flatMap { a =>
      coll.update
        .one(
          $id(a.id),
          $set(ActivityFields.racer -> { ~a.racer + score }),
          upsert = true
        )
        .void
    }

  def streak(userId: User.ID, score: Int): Funit =
    getOrCreate(userId) flatMap { a =>
      coll.update
        .one(
          $id(a.id),
          $set(ActivityFields.streak -> { ~a.streak + score }),
          upsert = true
        )
        .void
    }

  def learn(userId: User.ID, stage: String) =
    update(userId) { a =>
      a.copy(learn = Some(~a.learn + Learn.Stage(stage))).some
    }

  def practice(prog: lila.practice.PracticeProgress.OnComplete) =
    update(prog.userId) { a =>
      a.copy(practice = Some(~a.practice + prog.studyId)).some
    }

  def simul(simul: lila.simul.Simul) =
    simulParticipant(simul, simul.hostId) >>
      simul.pairings.map(_.player.user).map { simulParticipant(simul, _) }.sequenceFu.void

  def corresMove(gameId: Game.ID, userId: User.ID) =
    update(userId) { a =>
      a.copy(corres = Some((~a.corres).add(GameId(gameId), moved = true, ended = false))).some
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
    coll.secondaryPreferred.distinctEasy[User.ID, Set](
      "f.o.ids",
      regexId(from.id)
    ) flatMap { extra =>
      val all = following ++ extra
      all.nonEmpty.?? {
        logger.info(s"${from.id} unfollow ${all.size} users")
        all
          .map { userId =>
            coll.update.one(
              regexId(userId) ++ $doc("f.i.ids" -> from.id),
              $pull("f.i.ids" -> from.id)
            )
          }
          .sequenceFu
          .void
      }
    }

  def study(id: Study.Id) =
    studyApi byId id flatMap {
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

  def swiss(id: lila.swiss.Swiss.Id, ranking: lila.swiss.Ranking) =
    ranking.map { case (userId, rank) =>
      update(userId) { a => a.copy(swisses = Some(~a.swisses + SwissRank(id, rank))).some }
    }.sequenceFu

  private def simulParticipant(simul: lila.simul.Simul, userId: String) =
    update(userId) { a =>
      a.copy(simuls = Some(~a.simuls + SimulId(simul.id))).some
    }

  private def get(userId: User.ID)         = coll.byId[Activity, Id](Id today userId)
  private def getOrCreate(userId: User.ID) = get(userId) map { _ | Activity.make(userId) }
  private def save(activity: Activity)     = coll.update.one($id(activity.id), activity, upsert = true).void
  private def update(userId: User.ID)(f: Activity => Option[Activity]): Funit =
    getOrCreate(userId) flatMap { old =>
      f(old) ?? save
    }
}
