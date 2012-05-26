package lila
package forum

case class PostView(
    post: Post,
    topic: Topic,
    categ: Categ,
    pageOf: Post ⇒ Int) {

}
