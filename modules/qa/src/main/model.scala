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
    favoriters: Set[String],
    comments: List[Comment],
    views: Int,
    answers: Int,
    createdAt: DateTime,
    updatedAt: DateTime,
    acceptedAt: Option[DateTime],
    editedAt: Option[DateTime]) {

  def id = _id

  def slug = {
    val s = lila.common.String slugify title
    if (s.isEmpty) "-" else s
  }

  def withUser(user: User) = QuestionWithUser(this, user)
  def withUsers(user: User, commentsWithUsers: List[CommentWithUser]) =
    QuestionWithUsers(this, user, commentsWithUsers)

  def ownBy(user: User) = userId == user.id

  def withVote(f: Vote => Vote) = copy(vote = f(vote))

  def updateNow = copy(updatedAt = DateTime.now)
  def editNow = copy(editedAt = Some(DateTime.now)).updateNow

  def accepted = acceptedAt.isDefined

  def favorite(user: User): Boolean = favoriters(user.id)

  def setFavorite(userId: String, v: Boolean) = copy(
    favoriters = if (v) favoriters + userId else favoriters - userId
  )
}

case class QuestionWithUser(question: Question, user: User)
case class QuestionWithUsers(question: Question, user: User, comments: List[CommentWithUser])

case class Answer(
    _id: AnswerId,
    questionId: QuestionId,
    userId: String,
    body: String,
    vote: Vote,
    comments: List[Comment],
    acceptedAt: Option[DateTime],
    createdAt: DateTime,
    editedAt: Option[DateTime]) {

  def id = _id

  def accepted = acceptedAt.isDefined

  def withVote(f: Vote => Vote) = copy(vote = f(vote))

  def withUserAndComments(user: User, commentsWithUsers: List[CommentWithUser]) =
    AnswerWithUserAndComments(this, user, commentsWithUsers)

  def ownBy(user: User) = userId == user.id

  def editNow = copy(editedAt = Some(DateTime.now))
}

case class AnswerWithUser(answer: Answer, user: User)
case class AnswerWithUserAndComments(answer: Answer, user: User, comments: List[CommentWithUser])

case class AnswerWithQuestion(answer: Answer, question: QuestionWithUser)

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
    createdAt: DateTime) {

  def withUser(user: User) = CommentWithUser(this, user)
}

case class CommentWithUser(comment: Comment, user: User)

case class Profile(reputation: Int, questions: Int, answers: Int)

