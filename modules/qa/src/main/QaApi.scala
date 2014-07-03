package lila.qa

import scala.concurrent.duration._

import reactivemongo.bson._
import reactivemongo.core.commands.Count

import org.joda.time.DateTime
import spray.caching.{ LruCache, Cache }

import lila.common.paginator._
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.paginator._
import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

final class QaApi(questionColl: Coll, answerColl: Coll, mailer: Mailer) {

  object question {

    private implicit val commentBSONHandler = Macros.handler[Comment]
    private implicit val voteBSONHandler = Macros.handler[Vote]
    private[qa] implicit val questionBSONHandler = Macros.handler[Question]

    def create(data: Forms.QuestionData, user: User): Fu[Question] =
      lila.db.Util findNextId questionColl flatMap { id =>
        val q = Question(
          _id = id,
          userId = user.id,
          title = data.title,
          body = data.body,
          tags = data.tags,
          vote = Vote(Set.empty, Set.empty, 0),
          favoriters = Set.empty,
          comments = Nil,
          views = 0,
          answers = 0,
          createdAt = DateTime.now,
          updatedAt = DateTime.now,
          acceptedAt = None,
          editedAt = None)

        (questionColl insert q) >>
          tag.clearCache >>
          relation.clearCache inject q
      }

    def edit(data: Forms.QuestionData, id: QuestionId): Fu[Option[Question]] = findById(id) flatMap {
      case None => fuccess(none)
      case Some(q) =>
        val q2 = q.copy(title = data.title, body = data.body, tags = data.tags).editNow
        questionColl.update(BSONDocument("_id" -> q2.id), q2) >>
          tag.clearCache >>
          relation.clearCache inject Some(q2)
    }

    def findById(id: QuestionId): Fu[Option[Question]] =
      questionColl.find(BSONDocument("_id" -> id)).one[Question]

    def findByIds(ids: List[QuestionId]): Fu[List[Question]] =
      questionColl.find(BSONDocument("_id" -> BSONDocument("$in" -> ids.distinct))).cursor[Question].collect[List]()

    def accept(q: Question) = questionColl.update(
      BSONDocument("_id" -> q.id),
      BSONDocument("$set" -> BSONDocument("acceptedAt" -> DateTime.now))
    ) >> profile.clearCache

    def count: Fu[Int] = questionColl.db command Count(questionColl.name, None)

    def paginatorWithUsers(page: Int, perPage: Int): Fu[Paginator[QuestionWithUser]] =
      Paginator(
        adapter = new BSONAdapter[Question](
          collection = questionColl,
          selector = BSONDocument(),
          sort = BSONDocument("createdAt" -> -1)
        ) mapFutureList {
          (qs: Seq[Question]) => zipWithUsers(qs.toList)
        },
        currentPage = page,
        maxPerPage = perPage)

    def recent(max: Int): Fu[List[Question]] =
      questionColl.find(BSONDocument())
        .sort(BSONDocument("createdAt" -> -1))
        .cursor[Question].collect[List](max)

    def recentByUser(u: User, max: Int): Fu[List[Question]] =
      questionColl.find(BSONDocument("userId" -> u.id))
        .sort(BSONDocument("createdAt" -> -1))
        .cursor[Question].collect[List](max)

    def favoriteByUser(u: User, max: Int): Fu[List[Question]] =
      questionColl.find(BSONDocument("favoriters" -> u.id))
        .sort(BSONDocument("createdAt" -> -1))
        .cursor[Question].collect[List](max)

    def favoriters(q: Question): Fu[List[User]] = UserRepo byIds q.favoriters.toList

    def popular(max: Int): Fu[List[Question]] =
      questionColl.find(BSONDocument())
        .sort(BSONDocument("vote.score" -> -1))
        .cursor[Question].collect[List](max)

    def byTag(tag: String, max: Int): Fu[List[Question]] =
      questionColl.find(BSONDocument("tags" -> tag))
        .sort(BSONDocument("createdAt" -> -1))
        .cursor[Question].collect[List](max)

    def byTags(tags: List[String], max: Int): Fu[List[Question]] =
      questionColl.find(BSONDocument("tags" -> BSONDocument("$in" -> tags))).cursor[Question].collect[List](max)

    def zipWithUsers(questions: List[Question]): Fu[List[QuestionWithUser]] =
      UserRepo byIds questions.map(_.userId) map { users =>
        questions flatMap { question =>
          users find (_.id == question.userId) map question.withUser
        }
      }

    def withUsers(question: Question): Fu[Option[QuestionWithUsers]] = {
      val userIds = question.userId :: question.comments.map(_.userId)
      UserRepo byIds userIds map { users =>
        users find (_.id == question.userId) map { questionUser =>
          question.withUsers(questionUser, question.comments flatMap { comment =>
            users find (_.id == comment.userId) map comment.withUser
          })
        }
      }
    }

    def withUser(question: Question): Fu[Option[QuestionWithUser]] =
      UserRepo byId question.userId map {
        _ map { QuestionWithUser(question, _) }
      }

    def addComment(c: Comment)(q: Question) = questionColl.update(
      BSONDocument("_id" -> q.id),
      BSONDocument("$push" -> BSONDocument("comments" -> c)))

    def vote(id: QuestionId, user: User, v: Boolean): Fu[Option[Vote]] =
      question findById id flatMap {
        case Some(q) =>
          val newVote = q.vote.add(user.id, v)
          questionColl.update(
            BSONDocument("_id" -> q.id),
            BSONDocument("$set" -> BSONDocument("vote" -> newVote, "updatedAt" -> DateTime.now))
          ) >> profile.clearCache inject Some(newVote)
        case None => fuccess(none)
      }

    def favorite(id: QuestionId, user: User, v: Boolean): Fu[Option[Question]] =
      question findById id flatMap {
        case Some(q) =>
          val newFavs = q.setFavorite(user.id, v)
          questionColl.update(
            BSONDocument("_id" -> q.id),
            BSONDocument("$set" -> BSONDocument("favoriters" -> newFavs.favoriters, "updatedAt" -> DateTime.now))
          ) >> profile.clearCache inject Some(newFavs)
        case None => fuccess(none)
      }

    def incViews(q: Question) = questionColl.update(
      BSONDocument("_id" -> q.id),
      BSONDocument("$inc" -> BSONDocument("views" -> BSONInteger(1))))

    def recountAnswers(id: QuestionId) = answer.countByQuestionId(id) flatMap {
      setAnswers(id, _)
    }

    def setAnswers(id: QuestionId, nb: Int) = questionColl.update(
      BSONDocument("_id" -> id),
      BSONDocument(
        "$set" -> BSONDocument(
          "answers" -> BSONInteger(nb),
          "updatedAt" -> DateTime.now
        )
      )).void

    def remove(id: QuestionId) =
      questionColl.remove(BSONDocument("_id" -> id)) >>
        (answer removeByQuestion id) >>
        profile.clearCache >>
        tag.clearCache >>
        relation.clearCache

    def removeComment(id: QuestionId, c: CommentId) = questionColl.update(
      BSONDocument("_id" -> id),
      BSONDocument("$pull" -> BSONDocument("comments" -> BSONDocument("id" -> c)))
    )
  }

  object answer {

    private implicit val commentBSONHandler = Macros.handler[Comment]
    private implicit val voteBSONHandler = Macros.handler[Vote]
    private implicit val answerBSONHandler = Macros.handler[Answer]

    def create(data: Forms.AnswerData, q: Question, user: User): Fu[Answer] =
      lila.db.Util findNextId answerColl flatMap { id =>
        val a = Answer(
          _id = id,
          questionId = q.id,
          userId = user.id,
          body = data.body,
          vote = Vote(Set.empty, Set.empty, 0),
          comments = Nil,
          acceptedAt = None,
          createdAt = DateTime.now,
          editedAt = None)

        (answerColl insert a) >>
          (question recountAnswers q.id) >> {
            question favoriters q flatMap {
              mailer.createAnswer(q, a, user, _)
            }
          } inject a
      }

    def edit(data: Forms.AnswerData, id: AnswerId): Fu[Option[Answer]] = findById(id) flatMap {
      case None => fuccess(none)
      case Some(a) =>
        val a2 = a.copy(body = data.body).editNow
        answerColl.update(BSONDocument("_id" -> a2.id), a2) inject Some(a2)
    }

    def findById(id: AnswerId): Fu[Option[Answer]] =
      answerColl.find(BSONDocument("_id" -> id)).one[Answer]

    def accept(q: Question, a: Answer) = (question accept q) >> answerColl.update(
      BSONDocument("questionId" -> q.id),
      BSONDocument("$unset" -> BSONDocument("acceptedAt" -> true)),
      multi = true
    ) >> answerColl.update(
        BSONDocument("_id" -> a.id),
        BSONDocument("$set" -> BSONDocument("acceptedAt" -> DateTime.now))
      ) >> profile.clearCache

    def recentByUser(u: User, max: Int): Fu[List[Answer]] =
      answerColl.find(BSONDocument("userId" -> u.id))
        .sort(BSONDocument("createdAt" -> -1))
        .cursor[Answer].collect[List](max)

    def popular(questionId: QuestionId): Fu[List[Answer]] =
      answerColl.find(BSONDocument("questionId" -> questionId))
        .sort(BSONDocument("vote.score" -> -1))
        .cursor[Answer].collect[List]()

    def zipWithQuestions(answers: List[Answer]): Fu[List[AnswerWithQuestion]] =
      question.findByIds(answers.map(_.questionId)) flatMap question.zipWithUsers map { qs =>
        answers flatMap { a =>
          qs find (_.question.id == a.questionId) map { AnswerWithQuestion(a, _) }
        }
      }

    def zipWithUsers(answers: List[Answer]): Fu[List[AnswerWithUserAndComments]] = {
      val userIds = (answers.map(_.userId) ::: answers.flatMap(_.comments.map(_.userId)))
      UserRepo byIds userIds.distinct map { users =>
        answers flatMap { answer =>
          users find (_.id == answer.userId) map { answerUser =>
            val commentsWithUsers = answer.comments flatMap { comment =>
              users find (_.id == comment.userId) map comment.withUser
            }
            answer.withUserAndComments(answerUser, commentsWithUsers)
          }
        }
      }
    }

    def withUser(answer: Answer): Fu[Option[AnswerWithUser]] =
      UserRepo byId answer.userId map {
        _ map { AnswerWithUser(answer, _) }
      }

    def addComment(c: Comment)(a: Answer) = answerColl.update(
      BSONDocument("_id" -> a.id),
      BSONDocument("$push" -> BSONDocument("comments" -> c)))

    def vote(id: QuestionId, user: User, v: Boolean): Fu[Option[Vote]] =
      answer findById id flatMap {
        case Some(a) =>
          val newVote = a.vote.add(user.id, v)
          answerColl.update(
            BSONDocument("_id" -> a.id),
            BSONDocument("$set" -> BSONDocument("vote" -> newVote, "updatedAt" -> DateTime.now))
          ) >> profile.clearCache inject Some(newVote)
        case None => fuccess(none)
      }

    def remove(a: Answer): Fu[Unit] =
      answerColl.remove(BSONDocument("_id" -> a.id)) >>
        profile.clearCache >>
        (question recountAnswers a.questionId).void

    def remove(id: AnswerId): Fu[Unit] = findById(id) flatMap {
      case None    => funit
      case Some(a) => remove(a)
    }

    def removeByQuestion(id: QuestionId) =
      answerColl.remove(BSONDocument("questionId" -> id)) >> profile.clearCache

    def removeComment(id: QuestionId, c: CommentId) = answerColl.update(
      BSONDocument("questionId" -> id),
      BSONDocument("$pull" -> BSONDocument("comments" -> BSONDocument("id" -> c))),
      multi = true)

    def countByQuestionId(id: QuestionId) =
      answerColl.db command Count(answerColl.name, Some(BSONDocument("questionId" -> id)))
  }

  object comment {

    def create(data: Forms.CommentData, subject: Either[Question, Answer], user: User): Fu[Comment] = {
      val c = Comment(
        id = ornicar.scalalib.Random nextStringUppercase 8,
        userId = user.id,
        body = data.body,
        createdAt = DateTime.now)
      subject.fold(question addComment c, answer addComment c) >> {
        subject match {
          case Left(q) => funit
          case Right(a) => question findById a.questionId flatMap {
            case None    => funit
            case Some(q) => mailer.createAnswerComment(q, a, c, user)
          }
        }
      } inject c
    }

    def remove(questionId: QuestionId, commentId: CommentId) =
      question.removeComment(questionId, commentId) >>
        answer.removeComment(questionId, commentId)

    private implicit val commentBSONHandler = Macros.handler[Comment]
  }

  object tag {

    private val cache: Cache[List[Tag]] = LruCache(timeToLive = 1.day)

    def clearCache = fuccess(cache.clear)

    // list all tags found in questions collection
    def all: Fu[List[Tag]] = cache(true) {
      import reactivemongo.core.commands._
      val command = Aggregate(questionColl.name, Seq(
        Project("tags" -> BSONBoolean(true)),
        Unwind("tags"),
        Group(BSONBoolean(true))("tags" -> AddToSet("$tags"))
      ))
      questionColl.db.command(command) map {
        _.headOption flatMap {
          _.getAs[List[String]]("tags")
        } getOrElse Nil
      }
    }
  }

  object profile {

    private val cache: Cache[Profile] = LruCache(timeToLive = 1.day)

    def clearCache = fuccess(cache.clear)

    def apply(u: User): Fu[Profile] = cache(u.id) {
      question.recentByUser(u, 300) zip answer.recentByUser(u, 500) map {
        case (qs, as) => Profile(
          reputation = math.max(0, qs.map { q =>
            q.vote.score + q.favoriters.size
          }.sum + as.map { a =>
            a.vote.score + (if (a.accepted && !qs.exists(_.userId == a.userId)) 5 else 0)
          }.sum),
          questions = qs.size,
          answers = as.size)
      }
    }
  }

  object relation {

    private val questionsCache: Cache[List[Question]] = LruCache(timeToLive = 3.hours)

    def questions(q: Question, max: Int): Fu[List[Question]] = questionsCache(q.id -> max) {
      question.byTags(q.tags, 2000) map { qs =>
        qs.filter(_.id != q.id) sortBy { q2 =>
          -q.tags.union(q2.tags).size
        } take max
      }
    }

    def clearCache = fuccess(questionsCache.clear)

    // def tags(tag: Tag, max: Int): Fu[List[Tag]] = ???
  }
}
