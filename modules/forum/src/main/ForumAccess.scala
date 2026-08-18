package lila.forum

import lila.core.id.ForumCategId
import lila.core.perm.Granter
import lila.core.team.Access

final class ForumAccess(
    teamApi: lila.core.team.TeamApi,
    relationApi: lila.core.relation.RelationApi,
    usermod: UsermodApi
)(using Executor):

  enum Operation:
    case Read, Write

  private def isGranted(categId: ForumCategId, op: Operation)(using me: Option[Me]): Fu[Boolean] =
    ForumCateg
      .toTeamId(categId)
      .fold(fuTrue): teamId =>
        teamApi
          .forumAccessOf(teamId)
          .flatMap:
            case Access.None => fuFalse
            case Access.Everyone =>
              // when the team forum is open to everyone, you still need to belong to the team in order to post
              op match
                case Operation.Read => fuTrue
                case Operation.Write => me.soUse(teamApi.isMember(teamId))
            case Access.Members => me.soUse(teamApi.isMember(teamId))
            case Access.Leaders => me.soUse(teamApi.isLeader(teamId))

  def isGrantedRead(categId: ForumCategId)(using me: Option[Me]): Fu[Boolean] =
    if Granter.opt(_.Shusher) then fuTrue
    else isGranted(categId, Operation.Read)

  def isGrantedWrite(categId: ForumCategId, tryingToPostAsMod: Boolean = false)(using
      me: Option[Me]
  ): Fu[Boolean] =
    if tryingToPostAsMod && Granter.opt(_.Shusher) then fuTrue
    else if !canWriteInAnyForum then fuFalse
    else if categId == ForumCateg.diagnosticId || Granter.opt(_.ModerateForum) then
      isGranted(categId, Operation.Write)
    else
      me.fold(fuFalse): me =>
        usermod
          .isTimedOut(me.userId)
          .flatMap: timedOut =>
            if timedOut then fuFalse
            else isGranted(categId, Operation.Write)

  private def canWriteInAnyForum(using me: Option[Me]) = me.exists: me =>
    !me.isBot && {
      (me.count.game > 0 && me.createdSinceDays(2)) || me.hasTitle || me.isVerified || me.isPatron
    }

  def isGrantedMod(categId: ForumCategId)(using meOpt: Option[Me]): Fu[Boolean] = meOpt.soUse:
    if Granter.opt(_.ModerateForum) then fuTrue
    else ForumCateg.toTeamId(categId).so(teamApi.hasCommPerm)

  def isReplyBlockedOnUBlog(topic: ForumTopic, canModCateg: Boolean)(using me: Me): Fu[Boolean] =
    (topic.ublogId.isDefined && !canModCateg).so:
      topic.userId.so: topicAuthor =>
        relationApi.fetchBlocks(topicAuthor, me)
