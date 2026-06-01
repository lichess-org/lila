package lila.search

case class SearchChunk[A](
    results: Seq[A],
    nextCursor: Option[Long],
    unfilteredTotal: Long
)

final class SearchCursor[Id](
    countUnfiltered: Fu[Long],
    search: (Long, Long) => Fu[List[Id]],
    maxPerPage: MaxPerPage,
    overfetch: Int = 2
)(using Executor):

  def apply[A](
      cursor: Option[Long],
      filteredFetch: Seq[Id] => Fu[Map[Id, A]]
  ): Fu[SearchChunk[A]] =
    val pageSize = maxPerPage.value
    val batchSize = pageSize * overfetch.atLeast(1).toLong
    val startOffset = cursor.getOrElse(0L).atLeast(0L)

    def loop(
        offset: Long,
        fetchesLeft: Int,
        accepted: Vector[A]
    ): Fu[(Vector[A], Option[Long])] =
      if accepted.size >= pageSize then fuccess(accepted -> offset.some)
      else if fetchesLeft <= 0 then fuccess(accepted -> offset.some)
      else
        search(offset, batchSize).flatMap: ids =>
          if ids.isEmpty then fuccess(accepted -> none)
          else
            filteredFetch(ids).flatMap: fetchedById =>
              val slots = pageSize - accepted.size
              // advance to the raw search offset after the last result inside the requested page boundary
              val batchAccepted = ids.zipWithIndex.collect:
                // ids is the ordering. fetchedById decides which ids survive and what value they carry
                case (id, index) if fetchedById.contains(id) =>
                  fetchedById(id) -> (offset + index.toLong + 1L)
              val emitted = batchAccepted.take(slots)
              val nextAccepted = accepted ++ emitted.map(_._1)
              val fetchedUntil = offset + ids.size.toLong

              if batchAccepted.sizeIs > slots then fuccess(nextAccepted -> emitted.lastOption.map(_._2))
              else if nextAccepted.size >= pageSize then fuccess(nextAccepted -> fetchedUntil.some)
              else loop(fetchedUntil, fetchesLeft - 1, nextAccepted)

    for
      total <- countUnfiltered
      (results, nextCursor) <-
        if startOffset >= total then fuccess(Vector.empty -> none)
        else loop(startOffset, 20, Vector.empty) // 20 fetch limit is arbitrary protection against the unknown
    yield SearchChunk(results, nextCursor.filter(_ < total), total)
