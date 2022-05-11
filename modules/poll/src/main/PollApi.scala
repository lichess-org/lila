package lila.poll

import lila.db.dsl._
import reactivemongo.api.bson._

final class PollApi(
    repo: PollRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  // notify all subscribed to post on poll closed

  def create(p: Poll) = {}

  def close(p: Poll, href: String): Fu[Poll.Result] =
    repo.close(p._id) map { _ =>
      // notify voters "A poll you voted on has closed" with href
      Poll.Result(p)
    }

  def vote(p: Poll, uid: String, choice: Int) = {}
  /*
  def react(categSlug: String, postId: Post.ID, me: User, reaction: String, v: Boolean): Fu[Option[Post]] =
    Post.Reaction.set(reaction) ?? {
      if (v) lila.mon.forum.reaction(reaction).increment()
      postRepo.coll.ext
        .findAndUpdate[Post](
          selector = $id(postId) ++ $doc("categId" -> categSlug, "userId" $ne me.id),
          update = {
            if (v) $addToSet(s"reactions.$reaction" -> me.id)
            else $pull(s"reactions.$reaction"       -> me.id)
          },
          fetchNewObject = true
        )
    }
   */
}
