package lila.mod

import lila.db.api._
import lila.db.Implicits._
import tube.modlogTube
import play.api.libs.json.Json

final class ModlogApi {

  def engine(mod: String, user: String, v: Boolean) = add {
    Modlog(mod, user.some, v.fold(Modlog.engine, Modlog.unengine))
  }

  def troll(mod: String, user: String, v: Boolean) = add {
    Modlog(mod, user.some, v.fold(Modlog.troll, Modlog.untroll))
  }

  def ban(mod: String, user: String, v: Boolean) = add {
    Modlog(mod, user.some, v.fold(Modlog.ipban, Modlog.ipunban))
  }

  def closeAccount(mod: String, user: String) = add {
    Modlog(mod, user.some, Modlog.closeAccount)
  }

  def reopenAccount(mod: String, user: String) = add {
    Modlog(mod, user.some, Modlog.reopenAccount)
  }

  def setTitle(mod: String, user: String, title: Option[String]) = add {
    val name = title flatMap lila.user.User.titlesMap.get
    Modlog(mod, user.some, name.isDefined.fold(Modlog.setTitle, Modlog.removeTitle), details = name)
  }

  def ipban(mod: String, ip: String) = add {
    Modlog(mod, none, Modlog.ipban, ip.some)
  }

  def deletePost(mod: String, user: Option[String], author: Option[String], ip: Option[String], text: String) = add {
    Modlog(mod, user, Modlog.deletePost, details = Some(
      author.??(_ + " ") + ip.??(_ + " ") + text.take(140)
    ))
  }

  def toggleCloseTopic(mod: String, categ: String, topic: String, closed: Boolean) = add {
    Modlog(mod, none, closed ? Modlog.closeTopic | Modlog.openTopic, details = Some(
      categ + " / " + topic
    ))
  }

  def toggleHideTopic(mod: String, categ: String, topic: String, hidden: Boolean) = add {
    Modlog(mod, none, hidden ? Modlog.hideTopic | Modlog.showTopic, details = Some(
      categ + " / " + topic
    ))
  }

  def deleteQaQuestion(mod: String, user: String, title: String) = add {
    Modlog(mod, user.some, Modlog.deleteQaQuestion, details = Some(title take 140))
  }

  def deleteQaAnswer(mod: String, user: String, text: String) = add {
    Modlog(mod, user.some, Modlog.deleteQaAnswer, details = Some(text take 140))
  }

  def deleteQaComment(mod: String, user: String, text: String) = add {
    Modlog(mod, user.some, Modlog.deleteQaComment, details = Some(text take 140))
  }

  def recent = $find($query($select.all) sort $sort.naturalDesc, 100)

  def wasUnengined(userId: String) = $count.exists(Json.obj(
    "user" -> userId,
    "action" -> Modlog.unengine
  ))

  def assessGame(mod: String, gameId: String, side: String, assessment: Int) = add {
    val assessmentString = assessment match {
      case 1 => "Not cheating"
      case 2 => "Unlikely cheating"
      case 3 => "Unclear"
      case 4 => "Likely cheating"
      case 5 => "Cheating"
      case _ => "Not cheating"
    }
    Modlog(mod, none, Modlog.assessedGame, details = Some(gameId + "/" + side + " => " + assessmentString))
  }

  private def add(m: Modlog): Funit = {
    play.api.Logger("ModApi").info(m.toString)
    $insert(m)
  }
}
