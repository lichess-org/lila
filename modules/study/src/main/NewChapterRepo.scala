package lila.study

import akka.stream.scaladsl.*
import chess.format.pgn.Tags
import chess.format.UciPath
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*

import lila.db.AsyncColl
import lila.db.dsl.{ *, given }

import Node.{ BsonFields => F }

final class NewChapterRepo(val coll: AsyncColl)(using Executor, akka.stream.Materializer):

  import BSONHandlers.{ writeBranch, given }
  def byId(id: StudyChapterId): Fu[Option[NewChapter]] = coll(_.byId[NewChapter](id))

  def studyIdOf(chapterId: StudyChapterId): Fu[Option[StudyId]] =
    coll(_.primitiveOne[StudyId]($id(chapterId), "studyId"))
