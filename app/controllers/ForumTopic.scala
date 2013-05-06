package controllers

import lila.app._
import views._

object ForumTopic extends LilaController with ForumController {

  def form(categSlug: String) = TODO
  // Open { implicit ctx ⇒
  //   CategGrantWrite(categSlug) {
  //     IOptionOk(categRepo bySlug categSlug) { categ ⇒
  //       html.forum.topic.form(categ, forms.topic, forms.captchaCreate)
  //     }
  //   }
  // }

  def create(categSlug: String) = TODO
  // OpenBody { implicit ctx ⇒
  //   CategGrantWrite(categSlug) {
  //     implicit val req = ctx.body
  //     IOptionResult(categRepo bySlug categSlug) { categ ⇒
  //       forms.topic.bindFromRequest.fold(
  //         err ⇒ BadRequest(html.forum.topic.form(categ, err, forms.captchaCreate)),
  //         data ⇒ Firewall {
  //           val topic = topicApi.makeTopic(categ, data).unsafePerformIO
  //           Redirect(routes.ForumTopic.show(categ.slug, topic.slug, 1))
  //         }
  //       )
  //     }
  //   }
  // }

  def show(categSlug: String, slug: String, page: Int) = TODO
  // Open { implicit ctx ⇒
  //   CategGrantRead(categSlug) {
  //     IOptionOk(topicApi.show(categSlug, slug, page)) {
  //       case (categ, topic, posts) ⇒ {
  //         val form = (!posts.hasNextPage && isGrantedWrite(categSlug)) option forms.postWithCaptcha
  //         html.forum.topic.show(categ, topic, posts, form)
  //       }
  //     }
  //   }
  // }
}
