package lidraughts.draughtsnet

import akka.actor._
import akka.pattern.ask
import org.joda.time.DateTime

import lidraughts.hub.actorApi.draughtsnet.CommentaryEvent
import makeTimeout.short

private final class CommentDB(
    evalCache: DraughtsnetEvalCache,
    bus: lidraughts.common.Bus,
    system: ActorSystem
) {

  import Work.Commentary

  def add(comment: Commentary) = actor ! Add(comment)

  def acquire(client: Client): Fu[Option[Commentary]] =
    actor ? Acquire(client) mapTo manifest[Option[Commentary]]

  def postResult(
    commentId: Work.Id,
    client: Client,
    data: JsonApi.Request.PostCommentary
  ) =
    actor ! PostResult(commentId, client, data)

  def clean = actor ? Clean mapTo manifest[Iterable[Commentary]]

  private object Clean
  private case class Add(comment: Commentary)
  private case class Acquire(client: Client)
  private case class PostResult(
      commentId: Work.Id,
      client: Client,
      data: JsonApi.Request.PostCommentary
  )

  private val actor = system.actorOf(Props(new Actor {

    val coll = scala.collection.mutable.Map.empty[Work.Id, Commentary]

    val maxSize = 300

    def receive = {

      case Add(comment) if !coll.exists(_._2 similar comment) => coll += (comment.id -> comment)

      case Clean =>
        val since = DateTime.now minusSeconds 30
        val timedOut = coll.values.filter(_ acquiredBefore since)
        if (timedOut.nonEmpty) logger.debug(s"cleaning ${timedOut.size} of ${coll.size} comments")
        timedOut.foreach { m => updateOrGiveUp(m.timeout) }
        sender ! timedOut

      case Add(comment) =>
        clearIfFull
        coll += (comment.id -> comment)

      case Acquire(client) => sender ! coll.values.foldLeft(none[Commentary]) {
        case (found, m) if m.nonAcquired => Some {
          found.fold(m) { a =>
            if (m.canAcquire(client) && m.createdAt.isBefore(a.createdAt)) m else a
          }
        }
        case (found, _) => found
      }.map { m =>
        val comment = m assignTo client
        coll += (comment.id -> comment)
        comment
      }

      case PostResult(commentId, client, data) =>
        coll get commentId match {
          case None => logger.warn(s"Comment $commentId by $client not found")
          case Some(comment) if comment isAcquiredBy client =>
            if (data.commentary.pv.nonEmpty) {
              coll -= comment.id
              evalCache.putEval(comment, data.commentary) map { eval =>
                lidraughts.evalCache.JsonHandlers.gameEvalJson(comment.game.id, eval)
              } foreach { json =>
                bus.publish(
                  CommentaryEvent(comment.game.id, comment.game.simulId, json),
                  'draughtsnetComment
                )
              }
            } else {
              updateOrGiveUp(comment.invalid)
            }
          case Some(comment) => logger.warn(s"Comment $comment by $client was not acquired")
        }
    }

    def updateOrGiveUp(comment: Commentary) =
      if (comment.isOutOfTries) {
        logger.warn(s"Give up on comment $comment")
        coll -= comment.id
      } else coll += (comment.id -> comment)

    def clearIfFull =
      if (coll.size > maxSize) {
        logger.warn(s"CommentDB collection is full! maxSize=$maxSize. Dropping all now!")
        coll.clear()
      }
  }))
}
