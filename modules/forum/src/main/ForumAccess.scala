package lila.forum

import lila.core.id.ForumCategId
import lila.core.perm.Granter
import lila.core.team.Access

final class ForumAccess(
    teamApi: lila.core.team.TeamApi,
    relationApi: lila.core.relation.RelationApi
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
                case Operation.Write => me.so(teamApi.belongsTo(teamId, _))
            case Access.Members => me.so(teamApi.belongsTo(teamId, _))
            case Access.Leaders => me.so(teamApi.isLeader(teamId, _))

  def isGrantedRead(categId: ForumCategId)(using me: Option[Me]): Fu[Boolean] =
    if Granter.opt(_.Shusher) then fuTrue
    else isGranted(categId, Operation.Read)

  def isGrantedWrite(categId: ForumCategId, tryingToPostAsMod: Boolean = false)(using
      me: Option[Me]
  ): Fu[Boolean] =
    if tryingToPostAsMod && Granter.opt(_.Shusher) then fuTrue
    else canWriteInAnyForum.so(isGranted(categId, Operation.Write))

  private def canWriteInAnyForum(using me: Option[Me]) = me.exists: me =>
    !me.isBot && {
      (me.count.game > 0 && me.createdSinceDays(2)) || me.hasTitle || me.isVerified || me.isPatron
    }

  def isGrantedMod(categId: ForumCategId)(using meOpt: Option[Me]): Fu[Boolean] = meOpt.so: me =>
    if Granter.opt(_.ModerateForum) then fuTrue
    else ForumCateg.toTeamId(categId).so(teamApi.hasCommPerm(_, me))

  def isReplyBlockedOnUBlog(topic: ForumTopic, canModCateg: Boolean)(using me: Me): Fu[Boolean] =
    (topic.ublogId.isDefined && !canModCateg).so:
      topic.userId.so: topicAuthor =>
        relationApi.fetchBlocks(topicAuthor, me)
