package lila.studySearch

import play.api.libs.json.*

import lila.search.{ SearchClient, SearchApi }
import lila.search.SearchClient.*
import lila.study.{ Study, StudyRepo }
import lila.studySearch.StudySearchApi.*

final class StudySearchApi(elastic: SearchClient, studyRepo: StudyRepo)(using Executor)
    extends SearchApi[Study, Query]:

  def search(query: Query, offset: Long, length: Long) =
    elastic
      .searchIds(Index.Study, makeQuery(query), makeSort(query), offset, length, query)
      .flatMap: ids =>
        studyRepo.byOrderedIds(ids.map(StudyId.apply))

  def count(query: Query) = elastic.count(Index.Study, makeQuery(query), query)

  private def makeQuery(query: Query): JsObject =
    val parsed = parse(query.text, List("owner", "member"))
    parsed("owner").fold(makePublicQuery(query, parsed))(makeOwnerQuery(query, parsed))

  private def makeSort(query: Query): JsArray =
    JsArray(query.sorting.map(_.toElastic).toList :+ fieldSort("_score", "desc"))

  private case class ChapterClauses(textShould: List[JsObject], structuredMust: List[JsObject])

  private def chapterClauses(query: Query, termText: String, isOwnerQuery: Boolean): ChapterClauses =
    val nameDesc = if termText.isEmpty then Nil else List(chapterNameDescQuery(termText))
    query.chapter match
      case None if termText.nonEmpty && isOwnerQuery =>
        ChapterClauses(List(matchChapterQuery(termText)), Nil)
      case None => ChapterClauses(Nil, Nil)
      case Some(ChapterMode.SearchText) => ChapterClauses(nameDesc, Nil)
      case Some(ChapterMode.Filters(tagFilter)) => ChapterClauses(nameDesc, tagFilters(tagFilter))

  private def studyMatcher(parsed: ParsedQuery, textShould: List[JsObject]): JsObject =
    if parsed.terms.isEmpty then Json.obj("match_all" -> Json.obj())
    else boolQuery(should = textShould ++ matchStudyQueries(parsed.terms.mkString(" ")))

  private def chapterNameDescQuery(text: String): JsObject =
    nested(
      "chapters",
      multiMatch(
        text,
        List("chapters.name", "chapters.description"),
        analyzer = "english_with_chess_synonyms".some
      )
    )

  private def memberClause(parsed: ParsedQuery): List[JsObject] =
    parsed("member")
      .map(member => boolQuery(must = List(term(Fields.members, member))))
      .toList

  private def shouldClause(query: Query): List[JsObject] =
    List(selectPublic.some, query.userId.map(selectUserId)).flatten

  private def makePublicQuery(query: Query, parsed: ParsedQuery): JsObject =
    val clauses = chapterClauses(query, parsed.terms.mkString(" "), isOwnerQuery = false)
    boolQuery(
      must = studyMatcher(parsed, clauses.textShould) :: memberClause(parsed) ++ clauses.structuredMust,
      should = shouldClause(query),
      minimumShouldMatch = 1.some
    )

  private def makeOwnerQuery(query: Query, parsed: ParsedQuery)(owner: String): JsObject =
    val clauses = chapterClauses(query, parsed.terms.mkString(" "), isOwnerQuery = true)
    boolQuery(
      must = term(Fields.owner, owner) ::
        memberClause(parsed) ++
        (studyMatcher(parsed, clauses.textShould) :: clauses.structuredMust),
      should = shouldClause(query),
      minimumShouldMatch = 1.some
    )

  private def matchStudyQueries(text: String): List[JsObject] =
    List(
      multiMatch(
        text,
        List(s"${Fields.name}^3.0", s"${Fields.topics}^2.0", Fields.description),
        analyzer = "english_with_chess_synonyms".some,
        operator = "and".some
      ),
      multiMatch(text, List(Fields.owner, Fields.members))
    )

  private def matchChapterQuery(text: String): JsObject =
    nested(
      "chapters",
      boolQuery(
        should = List(
          multiMatch(
            text,
            List("chapters.name", "chapters.description"),
            analyzer = "english_with_chess_synonyms".some
          ),
          nested(
            "chapters.tags",
            boolQuery(
              should = List(
                term("chapters.tags.variant", text),
                term("chapters.tags.whiteFideId", text),
                term("chapters.tags.blackFideId", text),
                term("chapters.tags.eco", text),
                matchQuery("chapters.tags.event", text),
                matchQuery("chapters.tags.white", text),
                matchQuery("chapters.tags.black", text),
                matchQuery("chapters.tags.opening", text)
              ),
              minimumShouldMatch = 1.some
            )
          )
        )
      )
    )

  private def tagFilters(tagFilter: TagFilter): List[JsObject] =
    val tagQueries = List(
      tagFilter.variant.map(term("chapters.tags.variant", _)),
      tagFilter.eco.map(term("chapters.tags.eco", _)),
      tagFilter.opening.map(matchQuery("chapters.tags.opening", _)),
      tagFilter.event.map(matchQuery("chapters.tags.event", _))
    ).flatten ++
      symmetricPair(
        tagFilter.player1,
        tagFilter.player2,
        "chapters.tags.white",
        "chapters.tags.black",
        matchQuery
      ) ++
      symmetricPair(
        tagFilter.fideId1,
        tagFilter.fideId2,
        "chapters.tags.whiteFideId",
        "chapters.tags.blackFideId",
        term
      )

    if tagQueries.isEmpty then Nil
    else
      List(
        nested(
          "chapters",
          nested("chapters.tags", boolQuery(must = tagQueries))
        )
      )

  private def symmetricPair(
      first: Option[String],
      second: Option[String],
      whiteField: String,
      blackField: String,
      makeQuery: (String, String) => JsObject
  ): List[JsObject] =
    (first, second) match
      case (Some(firstValue), Some(secondValue)) =>
        List(
          boolQuery(
            should = List(
              boolQuery(must = List(makeQuery(whiteField, firstValue), makeQuery(blackField, secondValue))),
              boolQuery(must = List(makeQuery(whiteField, secondValue), makeQuery(blackField, firstValue)))
            )
          )
        )
      case _ =>
        List(first, second).flatten.map: value =>
          boolQuery(should = List(makeQuery(whiteField, value), makeQuery(blackField, value)))

  private val selectPublic = term(Fields.public, true)

  private def selectUserId(userId: String) = term(Fields.members, userId)

object StudySearchApi:
  // see file://./../../../../bin/elastic/study.ts

  case class Query(
      text: String,
      sorting: Option[Sorting],
      userId: Option[String],
      chapter: Option[ChapterMode] = None
  )

  case class Sorting(field: Field, order: Order):
    def toElastic: JsObject = fieldSort(field.elastic, order.elastic)

  enum Field(val elastic: String):
    case Name extends Field(s"${Fields.name}.${Fields.nameRaw}")
    case Likes extends Field(Fields.likes)
    case CreatedAt extends Field(Fields.createdAt)
    case UpdatedAt extends Field(Fields.updatedAt)
    case Hot extends Field(Fields.rank)

  enum Order(val elastic: String):
    case Asc extends Order("asc")
    case Desc extends Order("desc")

  enum ChapterMode:
    case SearchText
    case Filters(value: TagFilter)

  case class TagFilter(
      variant: Option[String] = None,
      eco: Option[String] = None,
      opening: Option[String] = None,
      player1: Option[String] = None,
      player2: Option[String] = None,
      fideId1: Option[String] = None,
      fideId2: Option[String] = None,
      event: Option[String] = None
  )

  object Fields:
    val name = "name"
    val nameRaw = "raw"
    val description = "description"
    val owner = "owner"
    val members = "members"
    val topics = "topics"
    val createdAt = "createdAt"
    val updatedAt = "updatedAt"
    val rank = "rank"
    val likes = "likes"
    val public = "public"
