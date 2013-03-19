package lila.notification

import lila.common.templating._
import play.api.templates.Html

object View {

  def view(id: String, from: Option[String] = None, closable: Boolean = true)(html: Html) = Html {
    s"""<div id="$id" class="notification">""" ++ 
  }

  @if(closable) {
  <a class="close" href="@routes.Notification.remove(id)">X</a>
  }
  @from.map { user =>
  @userIdLink(user, none)
  }
  <div class="inner">@html</div>
</div>
}
