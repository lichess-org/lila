package lila.search

import lila.search.client.{ SearchClient, SearchError }
import lila.search.spec.*

class LilaSearchClient(client: SearchClient, writeable: Boolean)(using Executor) extends SearchClient:

  override def storeBulkTeam(sources: List[TeamSourceWithId]): Future[Unit] =
    client.storeBulkTeam(sources)

  override def storeBulkStudy(sources: List[StudySourceWithId]): Future[Unit] =
    client.storeBulkStudy(sources)

  override def storeBulkGame(sources: List[GameSourceWithId]): Future[Unit] =
    client.storeBulkGame(sources)

  override def storeBulkForum(sources: List[ForumSourceWithId]): Future[Unit] =
    client.storeBulkForum(sources)

  override def store(id: String, source: Source): Future[Unit] =
    writeable.so:
      monitor("store", source.index):
        client.store(id, source)

  override def refresh(index: Index): Future[Unit] =
    client.refresh(index)

  override def mapping(index: Index): Future[Unit] =
    client.mapping(index)

  override def deleteById(index: Index, id: String): Future[Unit] =
    writeable.so(client.deleteById(index, id))

  override def deleteByIds(index: Index, ids: List[String]): Future[Unit] =
    writeable.so(client.deleteByIds(index, ids))

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

  extension (source: Source)
    def index = source match
      case s: Source.ForumCase => "forum"
      case s: Source.GameCase  => "game"
      case s: Source.StudyCase => "study"
      case s: Source.TeamCase  => "team"
