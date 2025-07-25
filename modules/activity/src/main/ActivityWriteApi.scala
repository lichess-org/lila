package lila.activity

import reactivemongo.api.bson.*

import lila.core.simul.Simul
import lila.db.AsyncCollFailingSilently
import lila.db.dsl.{ *, given }

final class ActivityWriteApi(
    withColl: AsyncCollFailingSilently,
    studyApi: lila.core.study.StudyApi,
    userApi: lila.core.user.UserApi
)(using Executor):

  import Activity.*
  import BSONHandlers.{ *, given }
  import activities.*

  def deleteAll(u: User): Funit = withColl(_.delete.one(regexId(u.id)).void)

  def game(game: Game): Funit =
    (for
      userId <- game.userIds
      player <- game.player(userId)
    yield update(userId): a =>
      val setGames = (!game.isCorrespondence).so(
        $doc(
          ActivityFields.games -> a.games.orZero
            .add(game.perfKey, Score.make(game.wonBy(player.color), RatingProg.make(player.light)))
        )
      )
      val setCorres = game.isCorrespondence.so(
        $doc(
          ActivityFields.corres -> a.corres.orZero.add(game.id, moved = false, ended = true)
        )
      )
      setGames ++ setCorres
    ).parallel.void

  def forumPost(post: lila.core.forum.ForumPostMini): Funit =
    post.userId
      .filterNot(_.is(UserId.lichess))
      .so: userId =>
        update(userId): a =>
          $doc(ActivityFields.forumPosts -> (~a.forumPosts + post.id))

  def ublogPost(post: lila.core.ublog.UblogPost): Funit = update(post.created.by): a =>
    $doc(ActivityFields.ublogPosts -> (~a.ublogPosts + post.id))

  def puzzle(res: lila.puzzle.Puzzle.UserResult): Funit = update(res.userId): a =>
    $doc(ActivityFields.puzzles -> {
      ~a.puzzles + Score.make(
        res = res.win.yes.some,
        rp = lila.core.rating.RatingProg(res.rating._1, res.rating._2).some
      )
    })

  def storm(userId: UserId, score: Int): Funit = update(userId): a =>
    $doc(ActivityFields.storm -> { ~a.storm + score })

  def racer(userId: UserId, score: Int): Funit = update(userId): a =>
    $doc(ActivityFields.racer -> { ~a.racer + score })

  def streak(userId: UserId, score: Int): Funit = update(userId): a =>
    $doc(ActivityFields.streak -> { ~a.streak + score })

  def learn(userId: UserId, stage: String) = update(userId): a =>
    $doc(ActivityFields.learn -> { ~a.learn + LearnStage(stage) })

  def practice(prog: lila.core.practice.OnComplete) = update(prog.userId): a =>
    $doc(ActivityFields.practice -> { ~a.practice + prog.studyId })

  def simul(simul: Simul): Funit =
    (simul.hostId :: simul.playerIds).sequentiallyVoid(simulParticipant(simul, _))

  def corresMove(gameId: GameId, userId: UserId) = update(userId): a =>
    $doc(ActivityFields.corres -> { (~a.corres).add(gameId, moved = true, ended = false) })

  def plan(userId: UserId, months: Int) = update(userId): _ =>
    $doc(ActivityFields.patron -> Patron(months))

  def follow(from: UserId, to: UserId) =
    update(from) { a =>
      $doc(ActivityFields.follows -> { (~a.follows).addOut(to) })
    } >>
      update(to): a =>
        $doc(ActivityFields.follows -> { (~a.follows).addIn(from) })

  def unfollowAll(from: User, following: Set[UserId]) =
    withColl: coll =>
      coll.secondary
        .distinctEasy[UserId, Set]("f.o.ids", regexId(from.id))
        .flatMap: extra =>
          val all = following ++ extra
          all.nonEmpty.so:
            logger.info(s"${from.id} unfollow ${all.size} users")
            all.toList.sequentiallyVoid: userId =>
              coll.update.one(
                regexId(userId) ++ $doc("f.i.ids" -> from.id),
                $pull("f.i.ids" -> from.id)
              )

  def study(id: StudyId) =
    studyApi
      .byId(id)
      .flatMap:
        _.filter(_.visibility == lila.core.study.Visibility.public).so: s =>
          userApi
            .isTroll(s.ownerId)
            .not
            .flatMapz:
              update(s.ownerId): a =>
                $doc(ActivityFields.studies -> { ~a.studies + s.id })

  def team(id: TeamId, userId: UserId) =
    update(userId): a =>
      $doc(ActivityFields.teams -> { ~a.teams + id })

  def streamStart(userId: UserId) =
    update(userId): _ =>
      $doc(ActivityFields.stream -> true)

  def swiss(id: SwissId, ranking: lila.core.swiss.Ranking) =
    ranking.toList.sequentiallyVoid: (userId, rank) =>
      update(userId): a =>
        $doc(ActivityFields.swisses -> { ~a.swisses + SwissRank(id, rank) })

  private def simulParticipant(simul: Simul, userId: UserId) = update(userId) { a =>
    $doc(ActivityFields.simuls -> { ~a.simuls + simul.id })
  }

  private def update(userId: UserId)(makeSetters: Activity => Bdoc): Funit =
    withColl: coll =>
      coll.byId[Activity](today(userId)).dmap(_ | Activity.make(userId)).flatMap { activity =>
        val setters = makeSetters(activity)
        setters.isEmpty.not.so:
          coll.update
            .one($id(activity.id), $set(setters), upsert = true)
            .flatMap:
              _.upserted.nonEmpty.so(truncateAfterInserting(coll, activity.id))
            .void
      }

  private def truncateAfterInserting(coll: Coll, id: Activity.Id): Funit = {
    // no need to do it every day
    (id.userId.hashCode % 3) == (id.day.value % 3)
  }.so:
    coll
      .find(regexId(id.userId), $id(true).some)
      .sort($sort.desc("_id"))
      .skip(Activity.recentNb)
      .one[Bdoc]
      .flatMap:
        _.flatMap(_.getAsOpt[Activity.Id]("_id")).so { oldId =>
          coll.delete
            .one:
              $doc(
                "_id" -> $doc(
                  "$lte" -> oldId,
                  "$regex" -> BSONRegex(s"^${id.userId}$idSep", "")
                )
              )
            .void
        }
