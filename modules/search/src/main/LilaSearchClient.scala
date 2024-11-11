package lila.search

import lila.search.client.{ SearchClient, SearchError }
import lila.search.spec.*

class LilaSearchClient(client: SearchClient)(using Executor) extends SearchClient:

  override def count(query: Query): Future[CountOutput] =
    monitor("count", query.index):
      client
        .count(query)
        .handleError:
          case e: SearchError =>
            logger.info(s"Count error: query={$query}", e)
            CountOutput(0)

  override def search(query: Query, from: From, size: Size): Future[SearchOutput] =
    monitor("search", query.index):
      client
        .search(query, from, size)
        .handleError:
          case e: SearchError =>
            logger.info(s"Search error: query={$query}, from={$from}, size={$size}", e)
            SearchOutput(Nil)

  private def monitor[A](op: String, index: String)(f: Fu[A]) =
    f.monTry(res => _.search.time(op, index, res.isSuccess))

  extension (query: Query)
    def index: String = query match
      case q: Query.Forum => "forum"
      case q: Query.Game  => "game"
      case q: Query.Study => "study"
      case q: Query.Team  => "team"
