package lila.study

import scala.concurrent.duration._

import cats.data.Validated

import lila.hub.actorApi.map.TellIfExists
import lila.user.User
import lila.game.Game
import lila.common.Bus

import shogi.Color

final class PostGameStudyApi(
    gameRepo: lila.game.GameRepo,
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo,
    studyMaker: StudyMaker,
    inviter: StudyInvite,
    cacheApi: lila.memo.CacheApi
)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private val cache = cacheApi.notLoading[Game.ID, Study.Id](64, "study.postGameStudyApi") {
    _.expireAfterAccess(5 minutes).buildAsync()
  }

  def studyWithOpponent(game: Game): Fu[Study.Id] =
    cache.getFuture(
      game.id,
      _ => {
        studyRepo
          .postGameStudyWithOpponent(game.id)
          .getOrElse(
            studyMaker
              .postGameStudy(
                game,
                Color.Sente,
                game.userIds.headOption.ifTrue(game.userIds.sizeIs == 1).getOrElse(User.lishogiId),
                game.userIds,
                withOpponent = true
              )
              .flatMap(scs => insertNewStudy(scs))
              .addEffect { s =>
                gameRepo.setPostGameStudy(game.id, s.id.value)
                Bus.publish(
                  TellIfExists(game.id, lila.hub.actorApi.round.PostGameStudy(s.id.value)),
                  "roundSocket"
                )
              }
          )
          .map(_.id)
      }
    )

  def study(game: Game, orientation: Color, invitedUser: Option[User], user: User): Fu[Study.Id] =
    studyMaker
      .postGameStudy(
        game,
        orientation,
        user.id,
        List(user.id.some, invitedUser.map(_.id)).flatten
      )
      .flatMap(scs => insertNewStudy(scs))
      .dmap(_.id)

  def getGameOfUser(gameId: Game.ID, user: User): Fu[Option[Game]] =
    getGame(gameId).map(_.filter(_.userIds.contains(user.id)))

  def getGame(gameId: Game.ID): Fu[Option[Game]] =
    gameRepo.finished(gameId)

  def getInvitedUser(invitedUsername: Option[String], user: User): Fu[Validated[String, Option[User]]] =
    invitedUsername.fold(fuccess(Validated.valid[String, Option[User]](none))) { i =>
      inviter
        .getInvitedUser(user, i)
        .fold(
          e => Validated.invalid[String, Option[User]](e.getMessage),
          user => Validated.valid[String, Option[User]](user.some)
        )
    }

  private def insertNewStudy(scs: Study.WithActualChapters): Fu[Study] =
    studyRepo.insert(scs.study) >>
      chapterRepo.bulkInsert(scs.chapters) >>-
      indexStudy(scs.study) inject scs.study

  private def indexStudy(study: Study) =
    Bus.publish(actorApi.SaveStudy(study), "study")
}
