package lila.study

import lila.core.notify.{ NotificationContent, NotifyApi }
import lila.core.perm.Granter
import lila.core.relation.Relation.{ Block, Follow }
import lila.db.dsl.{ *, given }

final private class StudyInvite(
    studyRepo: StudyRepo,
    userApi: lila.core.user.UserApi,
    notifyApi: NotifyApi,
    prefApi: lila.core.pref.PrefApi,
    relationApi: lila.core.relation.RelationApi
)(using Executor, lila.core.config.RateLimit):

  private val inviteLimit = lila.memo.RateLimit[UserId](
    credits = 400,
    duration = 1.day,
    key = "study.invite.user"
  )

  private val notifyRateLimit = lila.memo.RateLimit[UserId](
    credits = 100,
    duration = 1.day,
    key = "study.invite.notify.user"
  )

  private val maxMembers = 30

  def apply(
      byUserId: UserId,
      study: Study,
      invitedUsername: UserStr,
      getIsPresent: UserId => Fu[Boolean]
  ): Fu[User] = for
    _ <- (study.nbMembers >= maxMembers).so(fufail[Unit](s"Max study members reached: $maxMembers"))
    inviter <- userApi.me(byUserId).orFail("No such inviter")
    _ = if inviter.marks.isolate then fufail("You cannot invite new members to the study")
    given Me = inviter
    _ <- (!study.isOwner(inviter) && !Granter(_.StudyAdmin)).so:
      fufail[Unit]("Only the study owner can invite")
    invited <-
      userApi
        .enabledById(invitedUsername)
        .map(_.filterNot(u => UserId.lichess.is(u) && !Granter(_.StudyAdmin)))
        .orFail("No such invited")
    _ <- study.members.contains(invited).so(fufail[Unit]("Already a member"))
    relation <- relationApi.fetchRelation(invited.id, byUserId)
    _ <- relation.has(Block).so(fufail[Unit]("This user does not want to join"))
    isPresent <- getIsPresent(invited.id)
    _ <-
      if isPresent || Granter(_.StudyAdmin) then funit
      else
        prefApi
          .getStudyInvite(invited.id)
          .flatMap:
            case lila.core.pref.StudyInvite.ALWAYS => funit
            case lila.core.pref.StudyInvite.NEVER => fufail("This user doesn't accept study invitations")
            case lila.core.pref.StudyInvite.FRIEND =>
              if relation.has(Follow) then funit
              else fufail("This user only accept study invitations from friends")
    shouldNotify = !isPresent && (!inviter.marks.troll || relation.has(Follow))
    rateLimitCost =
      if Granter(_.StudyAdmin) then 0
      else if relation.has(Follow) then 1
      else if inviter.hasTitle then 2
      else if invited.hasTitle then 20
      else 10
    _ <- (!inviteLimit.zero(inviter.userId, rateLimitCost)(true))
      .so(fufail[Unit]("You have reach the study invite quota for the day"))
    _ <- studyRepo.addMember(study, StudyMember.make(invited))
    _ <- shouldNotify.so(notifyRateLimit.zero(inviter.userId, rateLimitCost):
      notifyApi.notifyOne(
        invited,
        NotificationContent.InvitedToStudy(
          invitedBy = inviter.userId,
          studyName = study.name,
          studyId = study.id
        )
      ))
  yield invited

  def becomeAdmin(me: MyId)(study: Study): Funit =
    studyRepo.coll:
      _.update
        .one(
          $id(study.id),
          $set(s"members.${me}" -> $doc("role" -> "w", "admin" -> true)) ++
            $addToSet("uids" -> me)
        )
        .void
