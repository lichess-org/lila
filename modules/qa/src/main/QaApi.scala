package lila.qa

import scala.concurrent.duration._

import reactivemongo.bson._

import org.joda.time.DateTime
import spray.caching.{ LruCache, Cache }

import lila.common.paginator._
import lila.db.BSON._
import lila.db.paginator._
import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

final class QaApi(
    questionColl: Coll,
    answerColl: Coll,
    mongoCache: lila.memo.MongoCache.Builder,
    notifier: Notifier) {

  object question {

    private implicit val commentBSONHandler = Macros.handler[Comment]
    private implicit val voteBSONHandler = Macros.handler[Vote]
    private[qa] implicit val questionBSONHandler = Macros.handler[Question]

    def create(data: QuestionData, user: User): Fu[Question] =
      lila.db.Util findNextId questionColl flatMap { id =>
        val q = Question(
          _id = id,
          userId = user.id,
          title = data.title,
          body = data.body,
          tags = data.tags,
          vote = Vote(Set(user.id), Set.empty, 1),
          comments = Nil,
          views = 0,
          answers = 0,
          createdAt = DateTime.now,
          updatedAt = DateTime.now,
          acceptedAt = None,
          editedAt = None)

        (questionColl insert q) >>
          tag.clearCache >>
          relation.clearCache >>-
          notifier.createQuestion(q, user) inject q
      }

    def edit(data: QuestionData, id: QuestionId): Fu[Option[Question]] = findById(id) flatMap {
      _ ?? { q =>
        val q2 = q.copy(title = data.title, body = data.body, tags = data.tags).editNow
        questionColl.update(BSONDocument("_id" -> q2.id), q2) >>
          tag.clearCache >>
          relation.clearCache inject q2.some
      }
    }

    def findById(id: QuestionId): Fu[Option[Question]] =
      questionColl.find(BSONDocument("_id" -> id)).one[Question]

    def findByIds(ids: List[QuestionId]): Fu[List[Question]] =
      questionColl.find(BSONDocument("_id" -> BSONDocument("$in" -> ids.distinct))).cursor[Question]().collect[List]()

    def accept(q: Question) = questionColl.update(
      BSONDocument("_id" -> q.id),
      BSONDocument("$set" -> BSONDocument("acceptedAt" -> DateTime.now))
    )

    def count: Fu[Int] = questionColl.count(None)

    def recentPaginator(page: Int, perPage: Int): Fu[Paginator[Question]] =
      paginator(BSONDocument(), BSONDocument("createdAt" -> -1), page, perPage)

    private def paginator(selector: BSONDocument, sort: BSONDocument, page: Int, perPage: Int): Fu[Paginator[Question]] =
      Paginator(
        adapter = new BSONAdapter[Question](
          collection = questionColl,
          selector = selector,
          projection = BSONDocument(),
          sort = sort
        ),
        currentPage = page,
        maxPerPage = perPage)

    private def popularCache = mongoCache(
      prefix = "qa:popular",
      f = (nb: Int) => questionColl.find(BSONDocument())
        .sort(BSONDocument("vote.score" -> -1))
        .cursor[Question]().collect[List](nb),
      timeToLive = 3 hour)

    def popular(max: Int): Fu[List[Question]] = popularCache(max)

    def byTag(tag: String, max: Int): Fu[List[Question]] =
      questionColl.find(BSONDocument("tags" -> tag.toLowerCase))
        .sort(BSONDocument("vote.score" -> -1))
        .cursor[Question]().collect[List](max)

    def byTags(tags: List[String], max: Int): Fu[List[Question]] =
      questionColl.find(BSONDocument("tags" -> BSONDocument("$in" -> tags.map(_.toLowerCase)))).cursor[Question]().collect[List](max)

    def addComment(c: Comment)(q: Question) = questionColl.update(
      BSONDocument("_id" -> q.id),
      BSONDocument("$push" -> BSONDocument("comments" -> c)))

    def vote(id: QuestionId, user: User, v: Boolean): Fu[Option[Vote]] =
      question findById id flatMap {
        _ ?? { q =>
          val newVote = q.vote.add(user.id, v)
          questionColl.update(
            BSONDocument("_id" -> q.id),
            BSONDocument("$set" -> BSONDocument("vote" -> newVote))
          ) inject newVote.some
        }
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

    def create(data: AnswerData, q: Question, user: User): Fu[Answer] =
      lila.db.Util findNextId answerColl flatMap { id =>
        val a = Answer(
          _id = id,
          questionId = q.id,
          userId = user.id,
          body = data.body,
          vote = Vote(Set(user.id), Set.empty, 1),
          comments = Nil,
          acceptedAt = None,
          createdAt = DateTime.now,
          editedAt = None)

        (answerColl insert a) >>
          (question recountAnswers q.id) >>-
          notifier.createAnswer(q, a, user) inject a
      }

    def edit(body: String, id: AnswerId): Fu[Option[Answer]] = findById(id) flatMap {
      _ ?? { a =>
        val a2 = a.copy(body = body).editNow
        answerColl.update(BSONDocument("_id" -> a2.id), a2) inject a2.some
      }
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
      )

    def popular(questionId: QuestionId): Fu[List[Answer]] =
      answerColl.find(BSONDocument("questionId" -> questionId))
        .sort(BSONDocument("vote.score" -> -1))
        .cursor[Answer]().collect[List]()

    def zipWithQuestions(answers: List[Answer]): Fu[List[AnswerWithQuestion]] =
      question.findByIds(answers.map(_.questionId)) map { qs =>
        answers flatMap { a =>
          qs find (_.id == a.questionId) map { AnswerWithQuestion(a, _) }
        }
      }

    def addComment(c: Comment)(a: Answer) = answerColl.update(
      BSONDocument("_id" -> a.id),
      BSONDocument("$push" -> BSONDocument("comments" -> c)))

    def vote(id: QuestionId, user: User, v: Boolean): Fu[Option[Vote]] =
      answer findById id flatMap {
        _ ?? { a =>
          val newVote = a.vote.add(user.id, v)
          answerColl.update(
            BSONDocument("_id" -> a.id),
            BSONDocument("$set" -> BSONDocument("vote" -> newVote))
          ) inject newVote.some
        }
      }

    def remove(a: Answer): Fu[Unit] =
      answerColl.remove(BSONDocument("_id" -> a.id)) >>
        (question recountAnswers a.questionId).void

    def remove(id: AnswerId): Fu[Unit] = findById(id) flatMap { _ ?? remove }

    def removeByQuestion(id: QuestionId) =
      answerColl.remove(BSONDocument("questionId" -> id))

    def removeComment(id: QuestionId, c: CommentId) = answerColl.update(
      BSONDocument("questionId" -> id),
      BSONDocument("$pull" -> BSONDocument("comments" -> BSONDocument("id" -> c))),
      multi = true)

    def moveToQuestionComment(a: Answer, q: Question) = {
      val allComments = Comment(
        id = Comment.makeId,
        userId = a.userId,
        body = a.body,
        createdAt = a.createdAt) :: a.comments
      allComments.map(c => question.addComment(c)(q)).sequenceFu >> remove(a)
    }

    def moveToAnswerComment(a: Answer, toAnswerId: AnswerId) =
      findById(toAnswerId) flatMap {
        _ ?? { toAnswer =>
          val allComments = Comment(
            id = Comment.makeId,
            userId = a.userId,
            body = a.body,
            createdAt = a.createdAt) :: a.comments
          allComments.map(c => addComment(c)(toAnswer)).sequenceFu >> remove(a)
        }
      }

    def countByQuestionId(id: QuestionId) =
      answerColl.count(Some(BSONDocument("questionId" -> id)))
  }

  object comment {

    def create(data: CommentData, subject: Either[Question, Answer], user: User): Fu[Comment] = {
      val c = Comment(
        id = Comment.makeId,
        userId = user.id,
        body = data.body,
        createdAt = DateTime.now)
      subject.fold(question addComment c, answer addComment c) >>- {
        subject match {
          case Left(q) => notifier.createQuestionComment(q, c, user)
          case Right(a) => question findById a.questionId foreach {
            _ foreach { q =>
              notifier.createAnswerComment(q, a, c, user)
            }
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
      val col = questionColl
      import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ AddToSet, Group, Project, Unwind }

      col.aggregate(Project(BSONDocument("tags" -> BSONBoolean(true))), List(
        Unwind("tags"), Group(BSONBoolean(true))("tags" -> AddToSet("tags")))).
        map(_.documents.headOption.flatMap(_.getAs[List[String]]("tags")).
          getOrElse(List.empty[String]).map(_.toLowerCase).distinct)
    }
  }

  object relation {
    private val questionsCache: Cache[List[Question]] =
      LruCache(timeToLive = 3.hours)

    def questions(q: Question, max: Int): Fu[List[Question]] = questionsCache(q.id -> max) {
      question.byTags(q.tags, 2000) map { qs =>
        qs.filter(_.id != q.id) sortBy { q2 =>
          -q.tags.union(q2.tags).size
        } take max
      }
    }

    def clearCache = fuccess(questionsCache.clear)
  }
}
