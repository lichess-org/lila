package lila.search

import lila.search.spec.*
import lila.search.client.{ SearchError, SearchClient }

class LilaSearchClient(client: SearchClient)(using Executor) extends SearchClient:

  override def storeBulkTeam(sources: List[TeamSourceWithId]): Future[Unit] =
    client.storeBulkTeam(sources)

  override def storeBulkStudy(sources: List[StudySourceWithId]): Future[Unit] =
    client.storeBulkStudy(sources)

  override def storeBulkGame(sources: List[GameSourceWithId]): Future[Unit] =
    client.storeBulkGame(sources)

  override def storeBulkForum(sources: List[ForumSourceWithId]): Future[Unit] =
    client.storeBulkForum(sources)

  override def store(id: String, source: Source): Future[Unit] =
    client.store(id, source)

  override def refresh(index: Index): Future[Unit] =
    client.refresh(index)

  override def mapping(index: Index): Future[Unit] =
    client.mapping(index)

  override def deleteById(index: Index, id: String): Future[Unit] =
    client.deleteById(index, id)

  override def deleteByIds(index: Index, ids: List[String]): Future[Unit] =
    client.deleteByIds(index, ids)

  override def count(query: Query): Future[CountOutput] =
    client
      .count(query)
      .handleError:
        case e: SearchError =>
          logger.warn(s"Count error: query={$query}", e)
          CountOutput(0)

  override def search(query: Query, from: Int, size: Int): Future[SearchOutput] =
    client
      .search(query, from, size)
      .handleError:
        case e: SearchError =>
          logger.warn(s"Search error: query={$query}, from={$from}, size={$size}", e)
          SearchOutput(Nil)
