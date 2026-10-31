package lila.studySearch

import play.api.libs.json.*

import lila.core.study.StudyOrder
import lila.search.SearchClient.*
import lila.search.{ SearchApi, SearchChunk, SearchClient, SearchCursor }
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

  def searchStudyDb(query: StudySearchQuery, userId: UserId): Fu[SearchChunk[StudyDbResult]] =
    val compiled = makeStudyDbQuery(query, userId)
    val publicOnly = query.study.isEmpty && query.chapter.isEmpty
    SearchCursor[StudyDbHit](
      countUnfiltered = elastic.count(Index.Study, compiled, query.copy(cursor = None)),
      search = (offset, length) =>
        if offset >= 10000 then fuccess(Nil)
        else
          elastic
            .searchHits(
              Index.Study,
              compiled,
              JsArray(
                (Sorting.fromStudyOrder(query.order).map(_.toElastic).toList ++ List(
                  fieldSort("_score", "desc"),
                  fieldSort(Fields.updatedAt, "desc"),
                  fieldSort(s"${Fields.name}.${Fields.nameRaw}", "asc")
                )).distinct
              ),
              offset,
              length.min(10000 - offset),
              query
            )
            .map(_.map(readStudyDbHit))
      ,
      maxPerPage = MaxPerPage(50)
    )(
      query.cursor,
      hits =>
        studyRepo
          .byOrderedIds(hits.map(_.id).toList)
          .map: studies =>
            val visible = studies
              .filter(study =>
                !study.isRelay &&
                  (study.isPublic || (!publicOnly && (study.isOwner(userId) || study.isMember(userId))))
              )
              .map(study => study.id -> study)
              .toMap
            hits
              .flatMap: hit =>
                visible
                  .get(hit.id)
                  .map(study =>
                    hit -> StudyDbResult(study, hit.matchingChapterIds, hit.matchingChapterFields)
                  )
              .toMap
    )

  private[studySearch] def makeStudyDbQuery(query: StudySearchQuery, userId: UserId): JsObject =
    def text(value: String, fields: List[String]) =
      multiMatch(
        value.trim,
        fields,
        analyzer = "english_with_chess_synonyms".some,
        operator = "and".some
      )
    val studyClauses = query.study.toList.flatMap: study =>
      List(
        study.name.map(text(_, List(Fields.name))),
        study.description.map(text(_, List(Fields.description))),
        study.topics.map(text(_, List(Fields.topics))),
        study.owner.map(value => term(Fields.owner, value.trim.toLowerCase)),
        study.member.map(value => term(Fields.members, value.trim.toLowerCase))
      ).flatten
    val chapterClauses = query.chapter.toList.flatMap: chapter =>
      val tagClauses = chapter.tags.toList.flatMap: tags =>
        tagQueries(
          tags.copy(
            variant = tags.variant.map(_.trim.toLowerCase),
            eco = tags.eco.map(_.trim.toUpperCase)
          )
        )
      List(
        chapter.text.map(text(_, List("chapters.name", "chapters.description"))),
        chapter.name.map(text(_, List("chapters.name"))),
        chapter.description.map(text(_, List("chapters.description")))
      ).flatten ++
        Option
          .when(tagClauses.nonEmpty):
            Json.obj(
              "nested" -> Json.obj(
                "path" -> "chapters.tags",
                "query" -> boolQuery(must = tagClauses),
                "inner_hits" -> Json.obj(
                  "name" -> "matching_tags",
                  "size" -> 1,
                  "_source" -> false,
                  "highlight" -> Json.obj("fields" -> Json.obj("chapters.tags.*" -> Json.obj()))
                )
              )
            )
          .toList
    def chapterQuery(clauses: List[JsObject], name: String): JsObject =
      Json.obj(
        "nested" -> Json.obj(
          "path" -> "chapters",
          "query" -> boolQuery(must = clauses),
          "inner_hits" -> Json.obj(
            "name" -> name,
            "size" -> Study.maxChapters.value,
            "_source" -> Json.arr("chapters.id"),
            "highlight" -> Json.obj(
              "fields" -> Json.obj("chapters.name" -> Json.obj(), "chapters.description" -> Json.obj())
            )
          )
        )
      )
    val chapterFilters = Option.when(chapterClauses.nonEmpty):
      chapterQuery(chapterClauses, "matching_chapters")
    val textClause = query.study
      .flatMap(_.text)
      .map: value =>
        boolQuery(
          should = List(
            boolQuery(
              must = text(value, List(s"${Fields.name}^3.0", s"${Fields.topics}^2.0", Fields.description)) ::
                chapterFilters.toList
            ),
            chapterQuery(
              text(value, List("chapters.name", "chapters.description")) :: chapterClauses,
              "matching_text_chapters"
            )
          ),
          minimumShouldMatch = 1.some
        )
    val clauses = studyClauses ++ textClause.orElse(chapterFilters).toList
    if clauses.isEmpty then boolQuery(filter = List(selectPublic))
    else
      boolQuery(
        must = clauses,
        should = List(selectPublic, selectUserId(userId.value)),
        minimumShouldMatch = 1.some
      )

  private def makeQuery(query: Query): JsObject =
    val parsed = parse(query.text, List("owner", "member"))
    parsed("owner").fold(makePublicQuery(query, parsed))(makeOwnerQuery(query, parsed))

  private def makeSort(query: Query): JsArray =
    JsArray(query.sorting.map(_.toElastic).toList :+ fieldSort("_score", "desc"))

  private case class ChapterClauses(textShould: List[JsObject], structuredMust: List[JsObject])

  private def chapterClauses(query: Query, termText: String, isOwnerQuery: Boolean): ChapterClauses =
    val nameDesc = if termText.isEmpty then Nil else List(chapterNameDescQuery(termText))
    query.chapter match
      case None if termText.nonEmpty && isOwnerQuery && StudySearchApi.ownerCompatibility =>
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
    val queries = tagQueries(tagFilter)
    if queries.isEmpty then Nil
    else List(nested("chapters", nested("chapters.tags", boolQuery(must = queries))))

  private def tagQueries(tagFilter: TagFilter): List[JsObject] =
    List(
      tagFilter.variant.map(term("chapters.tags.variant", _)),
      tagFilter.eco.map(term("chapters.tags.eco", _)),
      tagFilter.opening.map(matchQuery("chapters.tags.opening", _)),
      tagFilter.event.map(matchQuery("chapters.tags.event", _))
    ).flatten ++ playerQuery(tagFilter).toList

  private val selectPublic = term(Fields.public, true)

  private def selectUserId(userId: String) = term(Fields.members, userId)

object StudySearchApi:
  // see file://./../../../../bin/elastic/study.ts

  private val ownerCompatibility =
    sys.env.get("STUDY_OWNER_COMPATIBILITY").forall(_.toLowerCase != "false") // really?

  case class StudyDbResult(
      study: Study,
      matchingChapterIds: List[String],
      matchingChapterFields: Map[String, List[String]]
  )
  private[studySearch] case class StudyDbHit(
      id: StudyId,
      matchingChapterIds: List[String],
      matchingChapterFields: Map[String, List[String]]
  )

  private[studySearch] def readStudyDbHit(hit: JsObject): StudyDbHit =
    val chapters = List("matching_chapters", "matching_text_chapters").flatMap: name =>
      (hit \ "inner_hits" \ name \ "hits" \ "hits").asOpt[List[JsObject]].toList.flatten
    def fields(value: JsObject): List[String] =
      (value \ "highlight").asOpt[JsObject].toList.flatMap(_.keys).map(_.stripPrefix("chapters."))
    val matchingChapterFields = chapters.flatMap: chapter =>
      (chapter \ "_source" \ "id")
        .asOpt[String]
        .map: id =>
          val tags = (chapter \ "inner_hits" \ "matching_tags" \ "hits" \ "hits")
            .asOpt[List[JsObject]]
            .toList
            .flatten
          id -> (fields(chapter) ++ tags.flatMap(fields)).distinct.sorted
    StudyDbHit(
      StudyId((hit \ "_id").as[String]),
      chapters.flatMap(chapter => (chapter \ "_source" \ "id").asOpt[String]).distinct,
      matchingChapterFields.groupMap(_._1)(_._2).view.mapValues(_.flatten.distinct.sorted).toMap
    )

  case class Query(
      text: String,
      sorting: Option[Sorting],
      userId: Option[String],
      chapter: Option[ChapterMode] = None
  )

  case class Sorting(field: Field, order: Order):
    def toElastic: JsObject = fieldSort(field.elastic, order.elastic)

  object Sorting:
    def fromStudyOrder(order: StudyOrder): Option[Sorting] = order match
      case StudyOrder.alphabetical => Sorting(Field.Name, Order.Asc).some
      case StudyOrder.hot => Sorting(Field.Hot, Order.Desc).some
      case StudyOrder.newest => Sorting(Field.CreatedAt, Order.Desc).some
      case StudyOrder.oldest => Sorting(Field.CreatedAt, Order.Asc).some
      case StudyOrder.popular => Sorting(Field.Likes, Order.Desc).some
      case StudyOrder.updated => Sorting(Field.UpdatedAt, Order.Desc).some
      case StudyOrder.mine => Sorting(Field.Likes, Order.Asc).some
      case StudyOrder.relevant => none

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
      color1: Option[chess.Color] = None,
      color2: Option[chess.Color] = None,
      event: Option[String] = None
  )

  private[studySearch] def playerQuery(tags: TagFilter): Option[JsObject] =
    val firstActive = tags.player1.isDefined || tags.fideId1.isDefined
    val secondActive = tags.player2.isDefined || tags.fideId2.isDefined
    def clauses(name: Option[String], fideId: Option[String], color: chess.Color): List[JsObject] =
      val side = if color == chess.White then "white" else "black"
      name
        .map: value =>
          Json.obj(
            "match_bool_prefix" -> Json.obj(
              s"chapters.tags.$side" -> Json.obj("query" -> value.trim, "operator" -> "and")
            )
          )
        .toList ++ fideId.map(value => term(s"chapters.tags.${side}FideId", value.trim)).toList
    Option.when(firstActive || secondActive):
      val arrangements = List(chess.White, chess.Black).filter: firstColor =>
        (!firstActive || tags.color1.forall(_ == firstColor)) &&
          (!secondActive || tags.color2.forall(_ == !firstColor))
      if arrangements.isEmpty then Json.obj("match_none" -> Json.obj())
      else
        boolQuery(
          should = arrangements.map: firstColor =>
            boolQuery(
              must = clauses(tags.player1, tags.fideId1, firstColor) ++
                clauses(tags.player2, tags.fideId2, !firstColor)
            ),
          minimumShouldMatch = 1.some
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
