package lila.activity

import reactivemongo.api.bson._

import lila.db.AsyncCollFailingSilently
import lila.db.dsl._
import lila.game.Game
import lila.study.Study
import lila.user.User

final class ActivityWriteApi(
    withColl: AsyncCollFailingSilently,
    studyApi: lila.study.StudyApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Activity._
  import BSONHandlers._
  import activities._
  import model._

  def game(game: Game): Funit =
    (for {
      userId <- game.userIds
      pt     <- game.perfType
      player <- game playerByUserId userId
    } yield update(userId) { a =>
      val setGames = !game.isCorrespondence ?? $doc(
        ActivityFields.games -> a.games.orDefault
          .add(pt, Score.make(game wonBy player.color, RatingProg make player))
      )
      val setCorres = game.hasCorrespondenceClock ?? $doc(
        ActivityFields.corres -> a.corres.orDefault.add(GameId(game.id), moved = false, ended = true)
      )
      setGames ++ setCorres
    }).sequenceFu.void

  def forumPost(post: lila.forum.Post): Funit =
    post.userId.filter(User.lichessId !=) ?? { userId =>
      update(userId) { a =>
        $doc(ActivityFields.forumPosts -> (~a.forumPosts + ForumPostId(post.id)))
      }
    }

  def ublogPost(post: lila.ublog.UblogPost): Funit = update(post.created.by) { a =>
    $doc(ActivityFields.ublogPosts -> (~a.ublogPosts + UblogPostId(post.id.value)))
  }

  def puzzle(res: lila.puzzle.Puzzle.UserResult): Funit = update(res.userId) { a =>
    $doc(ActivityFields.puzzles -> {
      ~a.puzzles + Score.make(
        res = res.result.win.some,
        rp = RatingProg(Rating(res.rating._1), Rating(res.rating._2)).some
      )
    })
  }

  def storm(userId: User.ID, score: Int): Funit = update(userId) { a =>
    $doc(ActivityFields.storm -> { ~a.storm + score })
  }

  def racer(userId: User.ID, score: Int): Funit = update(userId) { a =>
    $doc(ActivityFields.racer -> { ~a.racer + score })
  }

  def streak(userId: User.ID, score: Int): Funit = update(userId) { a =>
    $doc(ActivityFields.streak -> { ~a.streak + score })
  }

  def learn(userId: User.ID, stage: String) = update(userId) { a =>
    $doc(ActivityFields.learn -> { ~a.learn + Learn.Stage(stage) })
  }

  def practice(prog: lila.practice.PracticeProgress.OnComplete) = update(prog.userId) { a =>
    $doc(ActivityFields.practice -> { ~a.practice + prog.studyId })
  }

  def simul(simul: lila.simul.Simul) =
    lila.common.Future.applySequentially(simul.hostId :: simul.pairings.map(_.player.user)) {
      simulParticipant(simul, _)
    }

  def corresMove(gameId: Game.ID, userId: User.ID) = update(userId) { a =>
    $doc(ActivityFields.corres -> { (~a.corres).add(GameId(gameId), moved = true, ended = false) })
  }

  def plan(userId: User.ID, months: Int) = update(userId) { a =>
    $doc(ActivityFields.patron -> Patron(months))
  }

  def follow(from: User.ID, to: User.ID) =
    update(from) { a =>
      $doc(ActivityFields.follows -> { ~a.follows addOut to })
    } >>
      update(to) { a =>
        $doc(ActivityFields.follows -> { ~a.follows addIn from })
      }

  def unfollowAll(from: User, following: Set[User.ID]) =
    withColl { coll =>
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
    }

  def study(id: Study.Id) =
    studyApi byId id flatMap {
      _.filter(_.isPublic) ?? { s =>
        update(s.ownerId) { a =>
          $doc(ActivityFields.studies -> { ~a.studies + s.id })
        }
      }
    }

  def team(id: String, userId: User.ID) =
    update(userId) { a =>
      $doc(ActivityFields.teams -> { ~a.teams + id })
    }

  def streamStart(userId: User.ID) =
    update(userId) { _ =>
      $doc(ActivityFields.stream -> true)
    }

  def swiss(id: lila.swiss.Swiss.Id, ranking: lila.swiss.Ranking) =
    lila.common.Future.applySequentially(ranking.toList) { case (userId, rank) =>
      update(userId) { a =>
        $doc(ActivityFields.swisses -> { ~a.swisses + SwissRank(id, rank) })
      }
    }

  private def simulParticipant(simul: lila.simul.Simul, userId: User.ID) = update(userId) { a =>
    $doc(ActivityFields.simuls -> { ~a.simuls + SimulId(simul.id) })
  }

  private def update(userId: User.ID)(makeSetters: Activity => Bdoc): Funit =
    withColl { coll =>
      coll.byId[Activity, Id](Id today userId).dmap { _ | Activity.make(userId) } flatMap { activity =>
        val setters = makeSetters(activity)
        !setters.isEmpty ?? {
          coll.update
            .one($id(activity.id), $set(setters), upsert = true)
            .flatMap {
              _.upserted.nonEmpty ?? truncateAfterInserting(coll, activity.id)
            }
            .void
        }
      }
    }

  private def truncateAfterInserting(coll: Coll, id: Activity.Id): Funit = {
    // no need to do it every day
    (id.userId.hashCode % 3) == (id.day.value % 3)
  } ?? coll
    .find(regexId(id.userId), $id(true).some)
    .sort($sort desc "_id")
    .skip(Activity.recentNb)
    .one[Bdoc]
    .flatMap {
      _.flatMap(_.getAsOpt[Activity.Id]("_id")) ?? { oldId =>
        coll.delete
          .one(
            $doc(
              "_id" -> $doc(
                "$lte"   -> oldId,
                "$regex" -> BSONRegex(s"^${id.userId}$idSep", "")
              )
            )
          )
          .void
      }
    }
}
