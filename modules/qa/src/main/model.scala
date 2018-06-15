package lila.qa

import org.joda.time._

import lila.user.User

case class Question(
    _id: QuestionId, // autoincrement integer
    userId: String,
    title: String,
    body: String, // markdown
    tags: List[String],
    vote: Vote,
    comments: List[Comment],
    views: Int,
    answers: Int,
    createdAt: DateTime,
    updatedAt: DateTime,
    acceptedAt: Option[DateTime],
    editedAt: Option[DateTime],
    locked: Option[Locked] = None
) {

  def id = _id

  def slug = {
    val s = lila.common.String slugify title
    if (s.isEmpty) "-" else s
  }

  def ownBy(user: User) = userId == user.id

  def withVote(f: Vote => Vote) = copy(vote = f(vote))

  def updateNow = copy(updatedAt = DateTime.now)
  def editNow = copy(editedAt = Some(DateTime.now)).updateNow

  def accepted = acceptedAt.isDefined

  def isLocked = locked.isDefined
}

case class Answer(
    _id: AnswerId,
    questionId: QuestionId,
    userId: String,
    body: String,
    vote: Vote,
    comments: List[Comment],
    acceptedAt: Option[DateTime],
    createdAt: DateTime,
    editedAt: Option[DateTime],
    modIcon: Option[Boolean]
) {

  def id = _id

  def accepted = acceptedAt.isDefined

  def withVote(f: Vote => Vote) = copy(vote = f(vote))

  def ownBy(user: User) = userId == user.id

  def editNow = copy(editedAt = Some(DateTime.now))

  def userIds = userId :: comments.map(_.userId)

  def displayModIcon = ~modIcon
}

case class AnswerWithQuestion(answer: Answer, question: Question)

case class Vote(up: Set[String], down: Set[String], score: Int) {

  def add(user: String, v: Boolean) = (if (v) addUp _ else addDown _)(user)
  def addUp(user: String) = copy(up = up + user, down = down - user).computeScore
  def addDown(user: String) = copy(up = up - user, down = down + user).computeScore

  def of(userId: String): Option[Boolean] =
    if (up(userId)) Some(true)
    else if (down(userId)) Some(false)
    else None

  private def computeScore = copy(score = up.size - down.size)
}

case class Comment(
    id: CommentId, // random string
    userId: String,
    body: String,
    createdAt: DateTime
) extends Ordered[Comment] {

  def compare(other: Comment) = createdAt.getSeconds compare other.createdAt.getSeconds
}

object Comment {

  def makeId = ornicar.scalalib.Random nextString 8
}

case class Locked(by: User.ID, at: DateTime)
