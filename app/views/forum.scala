package views.forum

import lila.app.templating.Environment.*

lazy val bits  = lila.forum.ui.ForumBits(helpers)(assetUrl)
lazy val post  = lila.forum.ui.PostUi(helpers, bits)
lazy val categ = lila.forum.ui.CategUi(helpers, bits)
lazy val topic = lila.forum.ui.TopicUi(helpers, bits, post)(
  views.base.captcha.apply,
  lila.msg.MsgPreset.forumDeletion.presets
)
