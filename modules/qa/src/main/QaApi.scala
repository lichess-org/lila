package lila.qa

import scala.concurrent.Future
import scala.concurrent.duration._

import reactivemongo.api.ReadConcern
import reactivemongo.bson._

import org.joda.time.DateTime

import lila.common.MaxPerPage
import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator._
import lila.security.Granter
import lila.user.User

final class QaApi(
    questionColl: Coll,
    answerColl: Coll,
    mongoCache: lila.memo.MongoCache.Builder,
    asyncCache: lila.memo.AsyncCache.Builder,
    notifier: Notifier
) {

  import QaApi._

  object question {

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
          editedAt = None
        )

        questionColl.insert.one(q) >>- {
          tag.clearCache
          relation.clearCache
          notifier.createQuestion(q, user)
        } inject q
      }

    def edit(data: QuestionData, id: QuestionId): Fu[Option[Question]] = findById(id) flatMap {
      _ ?? { q =>
        val q2 = q.copy(title = data.title, body = data.body, tags = data.tags).editNow
        questionColl.update.one($id(q2.id), q2) >>- {
          tag.clearCache
          relation.clearCache
        } inject q2.some
      }
    }

    def findById(id: QuestionId): Fu[Option[Question]] =
      questionColl.find($doc("_id" -> id)).one[Question]

    def findByIds(ids: List[QuestionId]): Fu[List[Question]] =
      questionColl.find($inIds(ids.distinct)).cursor[Question]().list

    def accept(q: Question) = questionColl.update.one(
      $doc("_id" -> q.id),
      $doc("$set" -> $doc("acceptedAt" -> DateTime.now))
    )

    def count: Fu[Int] = questionColl.count(selector = None, limit = None,
      skip = 0, hint = None, readConcern = ReadConcern.Local).map(_.toInt)

    def recentPaginator(page: Int, perPage: MaxPerPage): Fu[Paginator[Question]] =
      paginator($empty, $doc("createdAt" -> -1), page, perPage)

    private def paginator(selector: Bdoc, sort: Bdoc, page: Int, perPage: MaxPerPage): Fu[Paginator[Question]] =
      Paginator(
        adapter = new Adapter[Question](
          collection = questionColl,
          selector = selector,
          projection = $empty,
          sort = sort
        ),
        currentPage = page,
        maxPerPage = perPage
      )

    private def popularCache = mongoCache[Int, List[Question]](
      prefix = "qa:popular",
      f = nb => questionColl.find($empty)
        .sort($doc("vote.score" -> -1)).cursor[Question]().list(nb),
      timeToLive = 6 hour,
      keyToString = _.toString
    )

    def popular(max: Int): Fu[List[Question]] = popularCache(max)

    def byTag(tag: String, max: Int): Fu[List[Question]] =
      questionColl.find($doc("tags" -> tag.toLowerCase))
        .sort($doc("vote.score" -> -1)).cursor[Question]().list(max)

    def byTags(tags: List[String], max: Int): Fu[List[Question]] =
      questionColl.find($doc("tags" $in tags.map(_.toLowerCase)))
        .cursor[Question]().list(max)

    def addComment(c: Comment)(q: Question) =
      questionColl.update.one($id(q.id), $push("comments" -> c))

    def vote(id: QuestionId, user: User, v: Boolean): Fu[Option[Vote]] =
      question findById id flatMap {
        _ ?? { q =>
          val newVote = q.vote.add(user.id, v)

          questionColl.update.one(
            $id(q.id), $set("vote" -> newVote)
          ) inject newVote.some
        }
      }

    def incViews(q: Question) = questionColl.update
      .one($id(q.id), $inc("views" -> 1))

    def recountAnswers(id: QuestionId) = answer.countByQuestionId(id) flatMap {
      setAnswers(id, _)
    }

    def setAnswers(id: QuestionId, nb: Int): Funit =
      questionColl.update.one($id(id), $set(
        "answers" -> nb,
        "updatedAt" -> DateTime.now
      )).void

    def remove(id: QuestionId) = questionColl.delete.one($id(id)) >>
      (answer removeByQuestion id) >>- {
        tag.clearCache
        relation.clearCache
      }

    def removeComment(id: QuestionId, c: CommentId) =
      questionColl.update.one($id(id), $pull("comments" -> $doc("id" -> c)))

    def lock(id: QuestionId, by: Option[User]): Funit =
      questionColl.update.one($id(id), by match {
        case None => $unset("locked")
        case Some(u) => $set("locked" -> Locked(by = u.id, at = DateTime.now))
      }).void

    def erase(user: User): Funit = for {
      ids <- questionColl.distinct[QuestionId, List](
        "_id", $doc("userId" -> user.id)
      )

      delete = questionColl.delete
      ops <- (ids.nonEmpty) ?? {
        Future.sequence(Seq(
          delete.element($inIds(ids), Option.empty, Option.empty),
          delete.element($doc("questionId" $in ids), Option.empty, Option.empty)
        ))
      }
      _ <- delete.many(ops)
    } yield ()
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
          editedAt = None,
          modIcon = (~data.modIcon && Granter.apply(_.PublicMod)(user)).option(true)
        )

        answerColl.insert.one(a) >>
          (question recountAnswers q.id) >>-
          notifier.createAnswer(q, a, user) inject a
      }

    def edit(body: String, id: AnswerId): Fu[Option[Answer]] = findById(id) flatMap {
      _ ?? { a =>
        val a2 = a.copy(body = body).editNow
        answerColl.update.one($doc("_id" -> a2.id), a2) inject a2.some
      }
    }

    def findById(id: AnswerId): Fu[Option[Answer]] =
      answerColl.find($doc("_id" -> id)).one[Answer]

    def accept(q: Question, a: Answer) = (question accept q) >> answerColl
      .update.one(
        q = $doc("questionId" -> q.id),
        u = $doc("$unset" -> $doc("acceptedAt" -> true)),
        multi = true
      ) >> answerColl.update.one(
          $doc("_id" -> a.id),
          $doc("$set" -> $doc("acceptedAt" -> DateTime.now))
        )

    def popular(questionId: QuestionId): Fu[List[Answer]] =
      answerColl.find($doc("questionId" -> questionId))
        .sort($doc("vote.score" -> -1)).cursor[Answer]().list

    def zipWithQuestions(answers: List[Answer]): Fu[List[AnswerWithQuestion]] =
      question.findByIds(answers.map(_.questionId)) map { qs =>
        answers flatMap { a =>
          qs find (_.id == a.questionId) map { AnswerWithQuestion(a, _) }
        }
      }

    def addComment(c: Comment)(a: Answer) = answerColl.update.one(
      q = $doc("_id" -> a.id), u = $doc("$push" -> $doc("comments" -> c))
    )

    def vote(id: QuestionId, user: User, v: Boolean): Fu[Option[Vote]] =
      answer findById id flatMap {
        _ ?? { a =>
          val newVote = a.vote.add(user.id, v)
          answerColl.update.one(
            q = $doc("_id" -> a.id),
            u = $doc("$set" -> $doc("vote" -> newVote))
          ) inject newVote.some
        }
      }

    def remove(a: Answer): Fu[Unit] =
      answerColl.delete.one($doc("_id" -> a.id)) >>
        (question recountAnswers a.questionId).void

    def remove(id: AnswerId): Fu[Unit] = findById(id) flatMap { _ ?? remove }

    def removeByQuestion(id: QuestionId) =
      answerColl.delete.one($doc("questionId" -> id))

    def removeComment(id: QuestionId, c: CommentId) = answerColl.update.one(
      q = $doc("questionId" -> id),
      u = $doc("$pull" -> $doc("comments" -> $doc("id" -> c))),
      multi = true
    )

    def moveToQuestionComment(a: Answer, q: Question) = {
      val allComments = Comment(
        id = Comment.makeId,
        userId = a.userId,
        body = a.body,
        createdAt = a.createdAt
      ) :: a.comments
      allComments.map(c => question.addComment(c)(q)).sequenceFu >> remove(a)
    }

    def moveToAnswerComment(a: Answer, toAnswerId: AnswerId) =
      findById(toAnswerId) flatMap {
        _ ?? { toAnswer =>
          val allComments = Comment(
            id = Comment.makeId,
            userId = a.userId,
            body = a.body,
            createdAt = a.createdAt
          ) :: a.comments
          allComments.map(c => addComment(c)(toAnswer)).sequenceFu >> remove(a)
        }
      }

    def countByQuestionId(id: QuestionId): Fu[Int] =
      answerColl.countSel($doc("questionId" -> id))

    def erase(user: User) = answerColl.delete.one($doc("userId" -> user.id))
  }

  object comment {

    def create(data: CommentData, subject: Either[Question, Answer], user: User): Fu[Comment] = {
      val c = Comment(
        id = Comment.makeId,
        userId = user.id,
        body = data.body,
        createdAt = DateTime.now
      )
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

    private val cache = asyncCache.single(
      name = "qa.tags",
      f = fetch,
      expireAfter = _.ExpireAfterAccess(1 day)
    )

    def clearCache = cache.refresh

    // list all tags found in questions collection
    def all: Fu[List[Tag]] = cache.get

    private def fetch: Fu[List[Tag]] = {
      import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ AddFieldToSet, Group, Project, UnwindField }
      questionColl.aggregateOne(
        Project($doc("tags" -> true)),
        List(
          UnwindField("tags"), Group(BSONBoolean(true))("tags" -> AddFieldToSet("tags"))
        )
      ).map { doc =>
          (~doc.flatMap(_.getAs[List[String]]("tags"))).map(_.toLowerCase).distinct
        }
    }
  }

  object relation {
    private val cache = asyncCache.clearable(
      "qa.relation",
      f = fetch,
      expireAfter = _.ExpireAfterAccess(3 hours)
    )

    def questions(q: Question): Fu[List[Question]] = cache get q.id

    private def fetch(questionId: Int): Fu[List[Question]] =
      question.findById(questionId) flatMap {
        _ ?? { q =>
          question.byTags(q.tags, 2000) map { qs =>
            qs.filter(_.id != q.id) sortBy { q2 =>
              -q.tags.union(q2.tags).size
            } take 10
          }
        }
      }

    def clearCache = cache.invalidateAll
  }
}

object QaApi {

  implicit val commentBSONHandler: BSONDocumentHandler[Comment] = Macros.handler[Comment]
  implicit val voteBSONHandler: BSONDocumentHandler[Vote] = Macros.handler[Vote]
  implicit val lockedBSONHandler: BSONDocumentHandler[Locked] = Macros.handler[Locked]
  implicit val questionBSONHandler: BSONDocumentHandler[Question] = Macros.handler[Question]
}
