package lila.studySearch

import chess.format.pgn.Tag

import java.time.LocalDate

import lila.search.*
import lila.search.client.SearchClient
import lila.search.spec.{ Query, StudySource }
import lila.study.{ Study, StudyRepo }

final class StudySearchApi(
    client: SearchClient,
    studyRepo: StudyRepo
)(using Executor)
    extends SearchReadApi[Study, Query.Study]:

  def search(query: Query.Study, from: From, size: Size) =
    client
      .search(query, from, size)
      .flatMap: res =>
        studyRepo.byOrderedIds(res.hitIds.map(StudyId(_)))

  def count(query: Query.Study) = client.count(query).dmap(_.count)
