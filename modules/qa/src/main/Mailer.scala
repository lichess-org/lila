package lila.qa

import lila.common.String._
import lila.user.User

private[qa] final class Mailer(sender: String) {

  private[qa] def createAnswer(q: Question, a: Answer, u: User, favoriters: List[User]): Funit = ???
  // send(
  //       to = (rudyEmail :: user.email :: favoriters.map(_.email)) filterNot (u.email.==),
  //       subject = s"""${u.displaynameOrFullname} answered your question""",
  //       content = s"""New answer on prismic.io Q&A: ${questionUrl(q)}#answer-${a.id}


// By ${u.displaynameOrFullname}
// On question <b>${q.title}</b>

// ${a.body}

// URL: ${questionUrl(q)}#answer-${a.id}""")

  private[qa] def createQuestionComment(q: Question, c: Comment, u: User): Funit = ???
  // send(
  //       to = List(rudyEmail, questionAuthor.email) filterNot (u.email.==),
  //       subject = s"""${u.displaynameOrFullname} commented your question""",
  //       content = s"""New comment on prismic.io Q&A: ${questionUrl(question)}#comment-${c.id}


// By ${u.displaynameOrFullname}
// On question <b>${question.title}</b>

// ${c.body}

// URL: ${questionUrl(question)}#comment-${c.id}""")
  //     case _ => Future successful ()

  private[qa] def createAnswerComment(q: Question, a: Answer, c: Comment, u: User): Funit =
    ???
    // QaApi.answer.withUser(a) flatMap {
    //   case Some(AnswerWithUser(answer, answerAuthor)) => send(
    //     to = List(rudyEmail, answerAuthor.email) filterNot (u.email.==),
    //     subject = s"""${u.displaynameOrFullname} commented your answer""",
    //     content = s"""New comment on prismic.io Q&A: ${questionUrl(q)}#comment-${c.id}


// By ${u.displaynameOrFullname}
// On question <b>${q.title}</b>

// ${c.body}

// URL: ${questionUrl(q)}#comment-${c.id}""")

  private def questionUrl(q: Question) =
    s"http://lichess.org/qa/${q.id}/${q.slug}"

  private def send(to: List[String], subject: String, content: String) = {
    to foreach { recipient =>
      // common.utils.Mailer.send(
      //   to = recipient,
      //   from = Some(sender),
      //   fromname = Some(common.Wroom.domain),
      //   subject = s"[Q&A] $subject",
      //   content = Html(nl2br(content)))
    }
    fuccess(())
  }
}

