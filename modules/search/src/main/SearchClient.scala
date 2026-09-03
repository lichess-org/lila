package lila.search

import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.{ StandaloneWSClient, StandaloneWSResponse }
import scalalib.newtypes.SameRuntime

import scala.concurrent.duration.FiniteDuration

import lila.db.dsl.{ $doc, Coll }
import lila.mon.extensions.*

final class SearchClient(
    ws: StandaloneWSClient,
    endpoint: Url,
    eventColl: Coll,
    cacheApi: lila.memo.CacheApi
)(using Executor):
  import SearchClient.{ CountKey, Index }

  private val countCache = cacheApi[CountKey, Long](1024, "search.count"):
    _.expireAfterWrite(2.minutes).buildAsyncFuture(fetchCount)

  def searchIds(
      index: Index,
      query: JsObject,
      sort: JsArray,
      offset: Long,
      length: Long,
      context: => Any,
      timeout: Option[FiniteDuration] = None
  ): Fu[List[String]] =
    elastic("search", index, context, Nil)(
      ws.url(s"$endpoint/${index.esPath}/_search")
        .post(
          Json.obj(
            "query" -> query,
            "_source" -> false,
            "sort" -> sort,
            "from" -> offset,
            "size" -> length
          ) ++ timeout.fold(Json.obj())(duration => Json.obj("timeout" -> s"${duration.toMillis}ms"))
        )
    )(js => (js \ "hits" \ "hits").as[List[JsObject]].map(hit => (hit \ "_id").as[String]))

  def count(index: Index, query: JsObject, context: => Any): Fu[Long] =
    countCache.get(CountKey(index, query, context.toString))

  private def fetchCount(key: CountKey): Fu[Long] =
    elastic("count", key.index, key.context, 0L)(
      ws.url(s"$endpoint/${key.index.esPath}/_count")
        .post(Json.obj("query" -> key.query))
    )(js => (js \ "count").as[Long])

  def upsert[Id](index: Index, docId: Id)(using idAsString: SameRuntime[Id, String]): Funit =
    recordEvent(index, "upsert", idAsString(docId))

  def delete[Id](index: Index, docId: Id)(using idAsString: SameRuntime[Id, String]): Funit =
    recordEvent(index, "delete", idAsString(docId))

  private def elastic[A](op: "search" | "count", index: Index, context: => Any, fallback: A)(
      request: => Fu[StandaloneWSResponse]
  )(read: JsValue => A): Fu[A] =
    request
      .map:
        case res if res.status / 100 == 2 => read(res.body[JsValue])
        case res =>
          logger.info(
            s"Elasticsearch $op error: index={${index.esPath}}, query={$context}, ${res.status} ${res.body}"
          )
          fallback
      .recover:
        case e =>
          logger.info(s"Elasticsearch $op error: index={${index.esPath}}, query={$context}", e)
          fallback
      .monTry(res => lila.mon.search.time(op, index.name, res.isSuccess))

  private def recordEvent[Id](index: Index, operation: "upsert" | "delete", docId: String): Funit =
    eventColl.insert
      .one(
        $doc(
          "index" -> index.name,
          "docId" -> docId,
          "operation" -> operation,
          "createdAt" -> nowInstant
        )
      )
      .void

object SearchClient:

  private case class CountKey(index: Index, query: JsObject, context: String)

  enum Index:
    case Forum, Team, Ublog, Game, Study

    def name = toString.toLowerCase
    def esPath = if this == Study then "study_with_chapters" else name

  def bool(must: List[JsObject] = Nil, filter: List[JsObject] = Nil): JsObject =
    Json.obj(
      "bool" -> Json.obj(
        "must" -> JsArray(must),
        "filter" -> JsArray(filter)
      )
    )

  def boolQuery(
      must: List[JsObject] = Nil,
      should: List[JsObject] = Nil,
      filter: List[JsObject] = Nil,
      minimumShouldMatch: Option[Int] = None
  ): JsObject =
    val fields = Json.obj() ++
      (if must.nonEmpty then Json.obj("must" -> JsArray(must)) else Json.obj()) ++
      (if should.nonEmpty then Json.obj("should" -> JsArray(should)) else Json.obj()) ++
      (if filter.nonEmpty then Json.obj("filter" -> JsArray(filter)) else Json.obj()) ++
      minimumShouldMatch.map(value => Json.obj("minimum_should_match" -> value)).getOrElse(Json.obj())
    Json.obj("bool" -> fields)

  def compileFilter(queries: List[JsObject]): JsObject = queries match
    case Nil => Json.obj("match_all" -> Json.obj())
    case query :: Nil => query
    case _ => Json.obj("bool" -> Json.obj("filter" -> JsArray(queries)))

  def queryString(query: String, defaultField: String): JsObject =
    Json.obj(
      "query_string" -> Json.obj(
        "query" -> query,
        "default_field" -> defaultField
      )
    )

  def multiMatch(
      query: String,
      fields: List[String],
      analyzer: Option[String] = None,
      operator: Option[String] = None
  ): JsObject =
    Json.obj(
      "multi_match" -> (Json.obj(
        "query" -> query,
        "fields" -> fields
      ) ++ analyzer.map(value => Json.obj("analyzer" -> value)).getOrElse(Json.obj()) ++
        operator.map(value => Json.obj("operator" -> value)).getOrElse(Json.obj()))
    )

  def nested(path: String, query: JsObject): JsObject =
    Json.obj("nested" -> Json.obj("path" -> path, "query" -> query))

  def matchQuery(field: String, value: String): JsObject =
    Json.obj("match" -> Json.obj(field -> value))

  def term(field: String, value: String): JsObject =
    Json.obj("term" -> Json.obj(field -> value))

  def term(field: String, value: Boolean): JsObject =
    Json.obj("term" -> Json.obj(field -> value))

  def rangeGte(field: String, value: Int): JsObject =
    Json.obj("range" -> Json.obj(field -> Json.obj("gte" -> value)))

  def fieldSort(field: String, order: String, missing: Option[String] = None): JsObject =
    Json.obj(
      field -> (Json.obj("order" -> order) ++ missing.fold(Json.obj())(m => Json.obj("missing" -> m)))
    )

  def sanitizeQueryString(text: String, allowedFilters: Set[String] = Set.empty): String =
    text
      .trim()
      .toLowerCase()
      .replaceAll("""([\-=&|><!(){}\[\]^"~*?\\/])""", """\\$1""")
      .replaceAll(" and ", " AND ")
      .replaceAll("\\+", " AND ")
      .split("\\s+")
      .map:
        case term if allowedFilters.exists(term.matches) => term
        case term => term.replace(":", " ")
      .mkString(" ")

  case class ParsedQuery(terms: List[String], filters: Map[String, String]):
    def apply(key: String): Option[String] = filters.get(key)

  def parse(text: String, filterKeys: Seq[String]): ParsedQuery =
    val terms = "[ +]+".r.split(text.trim.toLowerCase).toList
    if filterKeys.isEmpty then ParsedQuery(terms, Map.empty)
    else
      terms.foldLeft(ParsedQuery(Nil, Map.empty)): (parsed, term) =>
        filterKeys
          .collectFirst:
            case key if term.startsWith(s"$key:") =>
              parsed.copy(filters = parsed.filters + (key -> term.drop(key.size + 1)))
          .getOrElse(parsed.copy(terms = parsed.terms :+ term))
