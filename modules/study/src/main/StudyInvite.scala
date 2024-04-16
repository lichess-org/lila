package lila.study

import lila.db.dsl.{ *, given }
import lila.core.notify.{ InvitedToStudy, NotifyApi }
import lila.core.relation.Relation.{ Block, Follow }
import lila.core.perm.Granter

final private class StudyInvite(
    studyRepo: StudyRepo,
    userApi: lila.core.user.UserApi,
    notifyApi: NotifyApi,
    prefApi: lila.core.pref.PrefApi,
    relationApi: lila.core.relation.RelationApi
)(using Executor):

  private val notifyRateLimit = lila.memo.RateLimit[UserId](
    credits = 500,
    duration = 1 day,
    key = "study.invite.user"
  )

  private val maxMembers = 30

  def apply(
      byUserId: UserId,
      study: Study,
      invitedUsername: UserStr,
      getIsPresent: UserId => Fu[Boolean]
  ): Fu[User] = for
    _       <- (study.nbMembers >= maxMembers).so(fufail[Unit](s"Max study members reached: $maxMembers"))
    inviter <- userApi.me(byUserId).orFail("No such inviter")
    given Me = inviter
    _ <- (!study.isOwner(inviter) && !Granter(_.StudyAdmin)).so:
      fufail[Unit]("Only the study owner can invite")
    invited <-
      userApi
        .enabledById(invitedUsername)
        .map(_.filterNot(u => UserId.lichess.is(u) && !Granter(_.StudyAdmin)))
        .orFail("No such invited")
    _         <- study.members.contains(invited).so(fufail[Unit]("Already a member"))
    relation  <- relationApi.fetchRelation(invited.id, byUserId)
    _         <- relation.has(Block).so(fufail[Unit]("This user does not want to join"))
    isPresent <- getIsPresent(invited.id)
    _ <-
      if isPresent || Granter(_.StudyAdmin) then funit
      else
        prefApi
          .getStudyInvite(invited.id)
          .flatMap:
            case lila.core.pref.StudyInvite.ALWAYS => funit
            case lila.core.pref.StudyInvite.NEVER  => fufail("This user doesn't accept study invitations")
            case lila.core.pref.StudyInvite.FRIEND =>
              if relation.has(Follow) then funit
              else fufail("This user only accept study invitations from friends")
    _ <- studyRepo.addMember(study, StudyMember.make(invited))
    shouldNotify = !isPresent && (!inviter.marks.troll || relation.has(Follow))
    rateLimitCost =
      if Granter(_.StudyAdmin) then 1
      else if relation.has(Follow) then 5
      else if inviter.hasTitle then 10
      else 100
    _ <- shouldNotify.so(notifyRateLimit.zero(inviter.userId, rateLimitCost):
      notifyApi.notifyOne(
        invited,
        InvitedToStudy(
          invitedBy = inviter.userId,
          studyName = study.name,
          studyId = study.id
        )
      )
    )
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
