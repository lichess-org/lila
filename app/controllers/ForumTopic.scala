package controllers

import lila._
import views._

import scalaz.effects._

object ForumTopic extends LilaController with Forum {

  def categApi = env.forum.topicApi
  def topicApi = env.forum.topicApi
  def categRepo = env.forum.categRepo
  def forms = env.forum.forms

  //def form(categSlug: String) = Open { implicit ctx ⇒
    //CategGrant(categSlug) {
      //IOptionOk(categRepo bySlug categSlug) { categ ⇒
        //val form = forms.TopicCaptcha(captchaCreate)
        //html.forum.topic.form(categ, form)
      //}
    //}
  //}

  //def create(categSlug: String) = OpenBody { implicit ctx ⇒
    //CategGrant(categSlug) {
      //implicit val req = ctx.body
      //IOptionResult(categRepo bySlug categSlug) { categ ⇒
        //forms.topic.bindFromRequest.fold(
          //err ⇒ BadRequest(html.forum.topic.form(categ, err)),
          //data ⇒ (for {
            //topic ← topicApi.makeTopic(categ, data, ctx.me)
          //} yield Redirect(routes.ForumCateg.show(categ.slug))
          //).unsafePerformIO
        //)
      //}
    //}
  //}
  def create(categSlug: String) = TODO
  def form(categSlug: String) = TODO

  def show(categSlug: String, slug: String, page: Int) = Open { implicit ctx ⇒
    CategGrant(categSlug) {
      IOptionOk(topicApi.show(categSlug, slug, page)) {
        case (categ, topic, posts) ⇒ {
          val form = (!posts.hasNextPage) option forms.postWithCaptcha
          html.forum.topic.show(categ, topic, posts, form)
        }
      }
    }
  }

  def delete(id: String) = TODO
}
