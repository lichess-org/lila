package views.forum

import lila.app.templating.Environment.*
import lila.forum.ui.*

lazy val bits  = ForumBits(helpers)
lazy val post  = PostUi(helpers, bits)
lazy val categ = CategUi(helpers, bits)
lazy val topic = TopicUi(helpers, bits, post)(
  views.base.captcha.apply,
  lila.msg.MsgPreset.forumDeletion.presets
)
