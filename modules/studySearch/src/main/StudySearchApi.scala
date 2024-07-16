package lila.studySearch

import akka.actor.*
import akka.stream.scaladsl.*
import chess.format.pgn.Tag

import java.time.LocalDate

import lila.common.LateMultiThrottler
import lila.search.*
import lila.search.client.SearchClient
import lila.search.spec.{ Query, StudySource }
import lila.study.{ Chapter, ChapterRepo, Study, StudyRepo }
import lila.tree.Node
import lila.tree.Node.Comments

final class StudySearchApi(
    client: SearchClient,
    indexThrottler: ActorRef,
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo
)(using Executor, Scheduler, akka.stream.Materializer)
    extends SearchReadApi[Study, Query.Study]:

  def search(query: Query.Study, from: From, size: Size) =
    client
      .search(query, from, size)
      .flatMap: res =>
        studyRepo.byOrderedIds(res.hitIds.map(StudyId(_)))

  def count(query: Query.Study) = client.count(query).dmap(_.count)

  def store(study: Study) = fuccess {
    indexThrottler ! LateMultiThrottler.work(
      id = study.id,
      run = studyRepo.byId(study.id).flatMapz(doStore),
      delay = 30.seconds.some
    )
  }

  private def doStore(study: Study) =
    getChapters(study)
      .flatMap { s =>
        client.storeStudy(s.study.id.value, toDoc(s))
      }
      .prefixFailure(study.id.value)

  private def toDoc(s: Study.WithActualChapters): StudySource =
    StudySource(
      name = s.study.name.value,
      owner = s.study.ownerId.value,
      members = s.study.members.ids.map(_.value).toList,
      chapterNames = s.chapters
        .collect { case c if !Chapter.isDefaultName(c.name) => c.name }
        .mkString(" "),
      chapterTexts = noMultiSpace {
        noKeyword {
          (s.study.description.toList :+ s.chapters.flatMap(chapterText)).mkString(" ")
        }
      },
      topics = s.study.topicsOrEmpty.value.map(_.value),
      // createdAt = study.createdAt,
      // updatedAt = study.updatedAt,
      likes = s.study.likes.value,
      public = s.study.isPublic
    )

  private val relevantPgnTags: Set[chess.format.pgn.TagType] = Set(
    Tag.Variant,
    Tag.Event,
    Tag.Round,
    Tag.White,
    Tag.Black,
    Tag.WhiteFideId,
    Tag.BlackFideId,
    Tag.ECO,
    Tag.Opening,
    Tag.Annotator
  )

  private def chapterText(c: Chapter): List[String] =
    nodeText(c.root) :: c.tags.value.collect {
      case Tag(name, value) if relevantPgnTags.contains(name) => value
    } ::: extraText(c)

  private def extraText(c: Chapter): List[String] =
    List(
      c.isPractice.option("practice"),
      c.isConceal.option("conceal puzzle"),
      c.isGamebook.option("lesson"),
      c.description
    ).flatten

  private def nodeText(n: Node): String =
    commentsText(n.comments) + " " + n.children.nodes.map(nodeText).mkString(" ")

  private def commentsText(cs: Comments): String =
    cs.value.map(_.text.value).mkString(" ")

  private def getChapters(s: Study): Fu[Study.WithActualChapters] =
    chapterRepo.orderedByStudyLoadingAllInMemory(s.id).map { Study.WithActualChapters(s, _) }

  private val multiSpaceRegex            = """\s{2,}""".r
  private def noMultiSpace(text: String) = multiSpaceRegex.replaceAllIn(text, " ")
  private val keywordRegex               = """@@\w+@@""".r
  private def noKeyword(text: String)    = keywordRegex.replaceAllIn(text, "")

  def reset(sinceStr: String) = {
    val sinceOption: Either[Unit, Option[LocalDate]] =
      if sinceStr == "reset" then Left(()) else Right(parseDate(sinceStr))
    val since = sinceOption match
      case Right(None) => sys.error("Missing since date argument")
      case Right(Some(date)) =>
        logger.info(s"Resume since $date")
        date
      case _ =>
        logger.info("Reset study index")
        client.mapping(index).await(10.seconds, "studyMapping")
        parseDate("2011-01-01").get
    logger.info(s"Index to ${index} since $since")
    val retryLogger = logger.branch("index")
    import lila.db.dsl.*
    Source
      .futureSource {
        studyRepo
          .sortedCursor(
            $doc("createdAt".$gte(since)),
            sort = $sort.asc("createdAt")
          )
          .map(_.documentSource())
      }
      .via(lila.common.LilaStream.logRate[Study]("study index")(logger))
      .mapAsyncUnordered(8) { study =>
        lila.common.LilaFuture.retry(() => doStore(study), 5 seconds, 10, retryLogger.some)
      }
      .runWith(Sink.ignore)
  } >> client.refresh(index)

  private def parseDate(str: String): Option[LocalDate] =
    val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
    scala.util.Try(java.time.LocalDate.parse(str, dateFormatter)).toOption
