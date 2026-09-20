package lila.search

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import java.util.function.Function

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch._types.{ FieldValue, SortOptions, SortOrder, query_dsl }
import co.elastic.clients.elasticsearch._types.query_dsl.{ MatchAllQuery, Operator }
import co.elastic.clients.elasticsearch.core.{ CountRequest, SearchRequest }
import co.elastic.clients.util.ObjectBuilder
import scalalib.newtypes.SameRuntime

import lila.db.dsl.{ bdoc, Coll }
import lila.mon.extensions.*

final class SearchClient(
    client: ElasticsearchAsyncClient,
    eventColl: Coll,
    cacheApi: lila.memo.CacheApi
)(using Executor):
  import SearchClient.*

  private val countCache = cacheApi[CountKey, Long](1024, "search.count"):
    _.expireAfterWrite(2.minutes).buildAsyncFuture(fetchCount)

  def searchIds(
      index: Index,
      query: SearchQuery,
      sort: List[SearchSort],
      offset: Long,
      length: Long,
      context: => Any,
      timeout: Option[FiniteDuration] = None
  ): Fu[List[String]] =
    val request =
      SearchRequest
        .Builder()
        .index(index.esPath)
        .query(query)
        .source(_.fetch(false))
        .sort(sort.asJava)
        .from(offset.toInt)
        .size(length.toInt)
    timeout.foreach(duration => request.timeout(s"${duration.toMillis}ms"))
    elastic("search", index, context, Nil):
      client
        .search(request.build(), classOf[Void])
        .asScala
        .map(_.hits().hits().asScala.toList.map(_.id()))

  def count(index: Index, query: SearchQuery, context: => Any): Fu[Long] =
    countCache.get(CountKey(index, query, context.toString))

  private def fetchCount(key: CountKey): Fu[Long] =
    val request = CountRequest.of(_.index(key.index.esPath).query(key.query))
    elastic("count", key.index, key.context, 0L):
      client.count(request).asScala.map(_.count())

  def upsert[Id](index: Index, docId: Id)(using idAsString: SameRuntime[Id, String]): Funit =
    recordEvent(index, "upsert", idAsString(docId))

  def delete[Id](index: Index, docId: Id)(using idAsString: SameRuntime[Id, String]): Funit =
    recordEvent(index, "delete", idAsString(docId))

  private def elastic[A](op: "search" | "count", index: Index, context: => Any, fallback: A)(
      request: => Fu[A]
  ): Fu[A] =
    request
      .recover:
        case e =>
          logger.info(s"es $op error: index={${index.esPath}}, query={$context}", e)
          fallback
      .monTry(res => lila.mon.search.time(op, index.name, res.isSuccess))

  private def recordEvent[Id](index: Index, operation: "upsert" | "delete", docId: String): Funit =
    eventColl.insert
      .one(
        bdoc(
          "index" -> index.name,
          "docId" -> docId,
          "operation" -> operation,
          "createdAt" -> nowInstant
        )
      )
      .void

object SearchClient:

  type SearchQuery = query_dsl.Query
  type SearchSort = SortOptions

  private def esQuery(build: Function[query_dsl.Query.Builder, ObjectBuilder[query_dsl.Query]]): SearchQuery =
    query_dsl.Query.of(build)

  private case class CountKey(index: Index, query: SearchQuery, context: String):
    override def equals(other: Any): Boolean = other match
      case that: CountKey => index == that.index && context == that.context
      case _ => false
    override def hashCode = 31 * index.hashCode + context.hashCode

  enum Index:
    case Forum, Team, Ublog, Game, Study

    def name = toString.toLowerCase
    def esPath = if this == Study then "study_with_chapters" else name

  def bool(must: List[SearchQuery] = Nil, filter: List[SearchQuery] = Nil): SearchQuery =
    boolQuery(must = must, filter = filter)

  def boolQuery(
      must: List[SearchQuery] = Nil,
      should: List[SearchQuery] = Nil,
      filter: List[SearchQuery] = Nil,
      minimumShouldMatch: Option[Int] = None
  ): SearchQuery =
    esQuery: query =>
      query.bool: bool =>
        if must.nonEmpty then bool.must(must.asJava)
        if should.nonEmpty then bool.should(should.asJava)
        if filter.nonEmpty then bool.filter(filter.asJava)
        minimumShouldMatch.foreach(value => bool.minimumShouldMatch(value.toString))
        bool

  def compileFilter(queries: List[SearchQuery]): SearchQuery = queries match
    case Nil => esQuery(_.matchAll((builder: MatchAllQuery.Builder) => builder))
    case query :: Nil => query
    case _ => boolQuery(filter = queries)

  def queryString(query: String, defaultField: String): SearchQuery =
    esQuery(_.queryString(_.query(query).defaultField(defaultField)))

  def multiMatch(
      query: String,
      fields: List[String],
      analyzer: Option[String] = None,
      operator: Option[String] = None
  ): SearchQuery =
    esQuery: root =>
      root.multiMatch: multiMatch =>
        multiMatch.query(query).fields(fields.asJava)
        analyzer.foreach(multiMatch.analyzer)
        operator.foreach(value => multiMatch.operator(Operator.valueOf(value.capitalize)))
        multiMatch

  def nested(path: String, query: SearchQuery): SearchQuery =
    esQuery(_.nested(_.path(path).query(query)))

  def matchQuery(field: String, value: String): SearchQuery =
    esQuery(_.`match`(_.field(field).query(value)))

  def term(field: String, value: String): SearchQuery =
    esQuery(_.term(_.field(field).value(value)))

  def term(field: String, value: Boolean): SearchQuery =
    esQuery(_.term(_.field(field).value(value)))

  def term(field: String, value: Int): SearchQuery =
    esQuery(_.term(_.field(field).value(value.toLong)))

  def terms(field: String, values: List[Int]): SearchQuery =
    esQuery(_.terms(_.field(field).terms(_.value(values.map(value => FieldValue.of(value.toLong)).asJava))))

  def numberRange(
      field: String,
      gt: Option[Double] = None,
      gte: Option[Double] = None,
      lte: Option[Double] = None
  ): SearchQuery =
    esQuery: query =>
      query.range: range =>
        range.number: bounds =>
          bounds.field(field)
          gt.foreach(value => bounds.gt(value))
          gte.foreach(value => bounds.gte(value))
          lte.foreach(value => bounds.lte(value))
          bounds

  def rangeGte(field: String, value: Int): SearchQuery =
    numberRange(field, gte = value.toDouble.some)

  def fieldSort(field: String, order: String, missing: Option[String] = None): SearchSort =
    SortOptions.of: sort =>
      sort.field: fieldSort =>
        fieldSort.field(field).order(SortOrder.valueOf(order.capitalize))
        missing.foreach(fieldSort.missing)
        fieldSort

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
