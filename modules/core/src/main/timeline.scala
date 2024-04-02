package lila.core
package timeline

case class ReloadTimelines(userIds: List[UserId])

sealed abstract class Atom(val channel: String, val okForKid: Boolean):
  def userIds: List[UserId]
case class Follow(u1: UserId, u2: UserId) extends Atom("follow", true):
  def userIds = List(u1, u2)
case class TeamJoin(userId: UserId, teamId: TeamId) extends Atom("teamJoin", false):
  def userIds = List(userId)
case class TeamCreate(userId: UserId, teamId: TeamId) extends Atom("teamCreate", false):
  def userIds = List(userId)
case class ForumPost(userId: UserId, topicId: ForumTopicId, topicName: String, postId: ForumPostId)
    extends Atom(s"forum:$topicId", false):
  def userIds = List(userId)
case class UblogPost(userId: UserId, id: UblogPostId, slug: String, title: String)
    extends Atom(s"ublog:$id", false):
  def userIds = List(userId)
case class TourJoin(userId: UserId, tourId: String, tourName: String) extends Atom("tournament", true):
  def userIds = List(userId)
case class GameEnd(fullId: GameFullId, opponent: Option[UserId], win: Option[Boolean], perf: String)
    extends Atom("gameEnd", true):
  def userIds = opponent.toList
case class SimulCreate(userId: UserId, simulId: SimulId, simulName: String) extends Atom("simulCreate", true):
  def userIds = List(userId)
case class SimulJoin(userId: UserId, simulId: SimulId, simulName: String) extends Atom("simulJoin", true):
  def userIds = List(userId)
case class StudyLike(userId: UserId, studyId: StudyId, studyName: StudyName) extends Atom("studyLike", true):
  def userIds = List(userId)
case class PlanStart(userId: UserId) extends Atom("planStart", true):
  def userIds = List(userId)
case class PlanRenew(userId: UserId, months: Int) extends Atom("planRenew", true):
  def userIds = List(userId)
case class BlogPost(id: String, slug: String, title: String) extends Atom("blogPost", true):
  def userIds = Nil
case class UblogPostLike(userId: UserId, id: String, title: String) extends Atom("ublogPostLike", false):
  def userIds = List(userId)
case class StreamStart(id: UserId, name: String) extends Atom("streamStart", false):
  def userIds = List(id)

enum Propagation:
  case Users(users: List[UserId])
  case Followers(user: UserId)
  case Friends(user: UserId)
  case WithTeam(teamId: TeamId)
  case ExceptUser(user: UserId)
  case ModsOnly(value: Boolean)

import Propagation.*

case class Propagate(data: Atom, propagations: List[Propagation] = Nil):
  def toUsers(ids: List[UserId])       = add(Users(ids))
  def toUser(id: UserId)               = add(Users(List(id)))
  def toFollowersOf(id: UserId)        = add(Followers(id))
  def toFriendsOf(id: UserId)          = add(Friends(id))
  def withTeam(teamId: Option[TeamId]) = teamId.fold(this)(id => add(WithTeam(id)))
  def exceptUser(id: UserId)           = add(ExceptUser(id))
  def modsOnly(value: Boolean)         = add(ModsOnly(value))
  private def add(p: Propagation)      = copy(propagations = p :: propagations)
