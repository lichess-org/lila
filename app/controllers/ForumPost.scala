package controllers

import lila.app._
import views._

object ForumPost extends LilaController with ForumController {

  def search(text: String, page: Int) = TODO
  // def search(text: String, page: Int) = OpenBody { implicit ctx ⇒
  //   text.trim match {
  //     case "" ⇒ Redirect(routes.ForumCateg.index)
  //     case text ⇒ Ok(html.forum.search(
  //       text,
  //       searchPaginator(text, page, isGranted(_.StaffForum))
  //     ))
  //   }
  // }

  def recent = TODO
  // Open { implicit ctx ⇒
  //   IOk(env.forum.recent(ctx.me, teamCache.teamIds) map { posts ⇒
  //     html.forum.post.recent(posts)
  //   })
  // }

  def create(categSlug: String, slug: String, page: Int) = TODO
  // OpenBody { implicit ctx ⇒
  //   CategGrantWrite(categSlug) {
  //     implicit val req = ctx.body
  //     IOptionResult(topicApi.show(categSlug, slug, page)) {
  //       case (categ, topic, posts) ⇒ forms.post.bindFromRequest.fold(
  //         err ⇒ BadRequest(html.forum.topic.show(
  //           categ, topic, posts, Some(err -> forms.captchaCreate))),
  //         data ⇒ Firewall {
  //           val post = postApi.makePost(categ, topic, data).unsafePerformIO
  //           Redirect("%s#%d".format(
  //             routes.ForumTopic.show(
  //               categ.slug,
  //               topic.slug,
  //               postApi lastPageOf topic.incNbPosts),
  //             post.number))
  //         }
  //       )
  //     }
  //   }
  // }

  def delete(id: String) = TODO 
  // Secure(Permission.ModerateForum) { implicit ctx ⇒
  //   me ⇒
  //     IOk(postApi.delete(id, me))
  // }
}
