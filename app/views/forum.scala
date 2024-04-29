package views

import lila.app.templating.Environment.*
import lila.forum.ui.*

object forum:
  val bits  = ForumBits(helpers)
  val post  = PostUi(helpers, bits)
  val categ = CategUi(helpers, bits)
  val topic = TopicUi(helpers, bits, post)(
    views.base.captcha.apply,
    lila.msg.MsgPreset.forumDeletion.presets
  )
