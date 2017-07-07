package lila.forum

import lila.db.BSON
import lila.db.BSON.LoggingHandler
import lila.db.dsl._
import reactivemongo.bson._
import org.joda.time.DateTime

private object BSONHandlers {

  implicit val CategBSONHandler = LoggingHandler(logger)(Macros.handler[Categ])

  implicit val PostEditBSONHandler = Macros.handler[OldVersion]
  implicit val PostBSONHandler = Macros.handler[Post]

  implicit val TopicBSONHandler = LoggingHandler(logger)(new BSONHandler[BSONDocument, Topic] {
    import Topic.BSONFields._

    def read(doc: BSONDocument): Topic = Topic(
      _id = doc.getAs[String](id).get,
      categId = doc.getAs[String](categId).get,
      slug = doc.getAs[String](slug).get,
      name = doc.getAs[String](name).get,
      views = doc.getAs[Int](views).get,
      createdAt = doc.getAs[DateTime](createdAt).get,
      updatedAt = doc.getAs[DateTime](updatedAt).get,
      nbPosts = doc.getAs[Int](nbPosts).get,
      lastPostId = doc.getAs[String](lastPostId).get,
      updatedAtTroll = doc.getAs[DateTime](updatedAtTroll).get,
      nbPostsTroll = doc.getAs[Int](nbPostsTroll).get,
      lastPostIdTroll = doc.getAs[String](lastPostIdTroll).get,
      troll = doc.getAs[Boolean](troll).get,
      closed = doc.getAs[Boolean](closed).get,
      hidden = doc.getAs[Boolean](hidden).get,
      sticky = doc.getAs[Boolean](sticky) getOrElse false
    )

    def write(o: Topic): BSONDocument = BSONDocument(
      id -> o.id,
      categId -> o.categId,
      slug -> o.slug,
      name -> o.name,
      views -> o.views,
      createdAt -> o.createdAt,
      updatedAt -> o.updatedAt,
      nbPosts -> o.nbPosts,
      lastPostId -> o.lastPostId,
      updatedAtTroll -> o.updatedAtTroll,
      nbPostsTroll -> o.nbPostsTroll,
      lastPostIdTroll -> o.lastPostIdTroll,
      troll -> o.troll,
      closed -> o.closed,
      hidden -> o.hidden,
      sticky -> o.sticky
    )
  })
}
