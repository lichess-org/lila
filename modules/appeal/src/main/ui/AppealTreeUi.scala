package lila.appeal
package ui

import lila.core.id.CmsPageKey
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class AppealTreeUi(helpers: Helpers, ui: AppealUi)(
    newAppeal: String => Context ?=> Frag
):
  import helpers.{ *, given }

  import trans.appeal as tap
  import lila.ui.navTree.*
  import lila.ui.navTree.Node.*

  private def cmsPageUrl(key: String) = routes.Cms.lonePage(CmsPageKey(key))

  private def cleanMenu(using Context): Branch =
    Branch(
      "root",
      tap.cleanAllGood(),
      List(
        Leaf(
          "clean-other-account",
          "I want to appeal for another account",
          frag(
            p(
              "Sorry we don't take appeals from other accounts. The appeal should come from nowhere else, but the concerned account."
            )
          )
        ),
        Leaf(
          "clean-warning",
          "I want to discuss a warning I received",
          frag(
            p(
              "Please note that warnings are only warnings, and that your account has not been restricted currently.",
              br,
              "If you still want to file an appeal, use the following form:"
            ),
            newAppeal("")
          )
        ),
        Leaf(
          "clean-other-issue",
          "I have another issue to discuss",
          p(
            "This channel of communication is for appealing moderation related issues.",
            br,
            "Please use ",
            a(href := routes.Main.contact)("the contact page"),
            " or ",
            a(href := "https://discord.gg/lichess")("our Discord server"),
            " to contact us about other issues.",
            br,
            "You can also ",
            a(href := cmsPageUrl("appeal"))("find here more information about appeals.")
          )
        )
      )
    )

  private def engineMenu(using Context): Branch =
    val accept =
      "I accept that I used external assistance in my games."
    val deny =
      "I deny having used external assistance in my games."
    Branch(
      "root",
      tap.engineMarked(),
      List(
        Leaf(
          "engine-accept",
          accept,
          frag(
            sendUsAnAppeal,
            newAppeal(s"$accept I am sorry and I would like another chance.")
          )
        ),
        Leaf(
          "engine-deny",
          deny,
          frag(
            p("You may send us an appeal, and a moderator will review it as soon as possible."),
            p(strong("What should I write in my appeal?")),
            p("Be honest and tell us the truth."),
            p(
              "Include everything that you think matters for your case. Only send your appeal once, and don't send any additional messages if they don't add anything important to your appeal. Sending additional messages will not get your appeal dealt with any sooner."
            ),
            p(
              "It is important to be honest from the start. If at first you deny doing anything wrong, we'll treat your appeal accordingly, and we will simply disregard any changes in your position. In other words, don't try to deny things at first only to confess to something later on."
            ),
            p(
              "Note that if your appeal is denied, you are not permitted to open additional accounts on Lichess."
            ),
            newAppeal(deny)
          )
        )
      ),
      content = tap.engineMarkedInfo(a(href := cmsPageUrl("fair-play"))(tap.fairPlay())).some
    )

  private def boostMenu(using Context): Branch =
    val accept = "I accept that I manipulated my rating."
    val acceptFull =
      "I accept that I deliberately manipulated my rating by losing games on purpose, or by playing another account that was deliberately losing games. I am sorry and I would like another chance."
    val deny =
      "I deny having manipulated my rating."
    val denyFull =
      "I deny having manipulated my rating. I have never lost rated games on purpose, or played several games with someone who does."
    Branch(
      "root",
      tap.boosterMarked(),
      List(
        Leaf(
          "boost-accept",
          accept,
          frag(
            sendUsAnAppeal,
            newAppeal(acceptFull)
          )
        ),
        Leaf(
          "boost-deny",
          deny,
          frag(
            sendUsAnAppeal,
            newAppeal(denyFull)
          )
        )
      ),
      content = tap.boosterMarkedInfo().some
    )

  private def muteMenu(using Context): Branch =
    val accept = "I accept that I have not followed the communication guidelines"
    val acceptFull =
      "I accept that I have not followed the communication guidelines. I will behave better in future, please give me another chance."
    val deny =
      "I have followed the communication guidelines"
    Branch(
      "root",
      tap.accountMuted(),
      List(
        Leaf(
          "mute-accept",
          accept,
          frag(
            p(
              "I accept that I have not followed the ",
              a(href := cmsPageUrl("communication-guidelines"))(
                "communication guidelines"
              ),
              ". I will behave better in future, please give me another chance."
            ),
            sendUsAnAppeal,
            newAppeal(acceptFull)
          )
        ),
        Leaf(
          "mute-deny",
          deny,
          frag(
            sendUsAnAppeal,
            newAppeal(deny)
          )
        )
      ),
      content = tap
        .accountMutedInfo(
          a(href := cmsPageUrl("communication-guidelines"))(tap.communicationGuidelines())
        )
        .some
    )

  private def rankBanMenu(using Context): Branch =
    val accept = "I accept that I have manipulated my account to get on the leaderboard."
    val deny =
      "I deny having manipulated my account to get on the leaderboard."
    Branch(
      "root",
      tap.excludedFromLeaderboards(),
      List(
        Leaf(
          "rankban-accept",
          accept,
          frag(
            sendUsAnAppeal,
            newAppeal(accept)
          )
        ),
        Leaf(
          "rankban-deny",
          deny,
          frag(
            sendUsAnAppeal,
            newAppeal(deny)
          )
        )
      ),
      content = tap.excludedFromLeaderboardsInfo().some
    )

  private def arenaBanMenu(using Context): Branch =
    val noPlay = "I have joined many arenas without playing in them"
    val noStart = "I did not move in many arenas games"
    val deny = "I have followed fair-play and arenas rules"
    Branch(
      "root",
      tap.arenaBanned(),
      List(
        Leaf(
          "arena-ban-no-play",
          noPlay,
          frag(
            sendUsAnAppeal,
            newAppeal(noPlay)
          )
        ),
        Leaf(
          "arena-ban-not-starting",
          noStart,
          frag(
            sendUsAnAppeal,
            newAppeal(noStart)
          )
        ),
        Leaf(
          "arena-ban-deny",
          deny,
          frag(
            sendUsAnAppeal,
            newAppeal(deny)
          )
        )
      )
    )

  private def hiddenBlogMenu(using Context): Branch =
    val accept =
      "I accept that I have broken the blog rules"
    val deny =
      "I deny having broken the blog rules."
    Branch(
      "root",
      tap.hiddenBlog(),
      List(
        Leaf(
          "hidden-blog-accept",
          accept,
          frag(
            sendUsAnAppeal,
            newAppeal(accept)
          )
        ),
        Leaf(
          "hidden-blog-deny",
          deny,
          frag(
            sendUsAnAppeal,
            newAppeal(deny)
          )
        )
      ),
      content = tap.hiddenBlogInfo(a(href := cmsPageUrl("blog-etiquette"))(tap.blogRules())).some
    )

  private def prizebanMenu(using Context): Branch =
    val prizebanExpired = "My ban duration has expired, as I was informed by moderators."
    val deny = "I reject any allegation of wrongdoing that may have prompted a prizeban."
    Branch(
      "root",
      tap.prizeBanned(),
      List(
        Leaf(
          "prizeban-expired",
          prizebanExpired,
          frag(
            sendUsAnAppeal,
            newAppeal(prizebanExpired)
          )
        ),
        Leaf(
          "prizeban-deny",
          deny,
          frag(
            sendUsAnAppeal,
            newAppeal(deny)
          )
        )
      )
    )

  private def playbanMenu(using Context): Branch =
    Branch(
      "root",
      tap.playTimeout(),
      List(
        Leaf(
          "playban-abort",
          "For having aborted too many games.",
          frag(
            p(
              "We understand your frustration, but temporary play bans for aborting too many games are necessary. It's very annoying for your opponent when the game gets aborted and we have to discourage it."
            ),
            p("A few things we can suggest are:"),
            ul(
              li("Don't send a challenge if you don't want to play and then abort the game."),
              li(
                "If you don't want to face lower or higher rated opponents, set a rating range on your seek."
              ),
              li(
                "Don't abort games if you want to have a particular color, you have to play with both colors."
              )
            )
          )
        ),
        Leaf(
          "playban-timeout",
          "For letting my game clock time run out.",
          p(
            "We understand your frustration, but temporary play bans for stalling in games are necessary, it can be very frustrating for opponents to waste time in lost positions before resigning."
          )
        ),
        Leaf(
          "playban-disconnect",
          "For frequently disconnecting from games.",
          frag(
            p(
              "We understand your frustration, but temporary play bans for losing connection are necessary, even if you don't disconnect on purpose. It's very annoying to suddenly lose your opponent during a game and we have to discourage it."
            ),
            p(
              "The only thing we can suggest to you is that you try to get a better connection or play longer time-control games that are more forgiving of disconnections."
            )
          )
        )
      )
    )

  private def altScreen(using Context) = div(cls := "leaf")(
    h2(tap.closedByModerators()),
    div(cls := "content")(
      p("Did you create multiple accounts? If so, remember that you promised not to, on the sign up page."),
      p(
        "If you violated the terms of service on a previous account, then you are not allowed to make a new one, ",
        "unless it was explicitly allowed by the moderation team during an appeal."
      ),
      p(
        "If you never violated the terms of service, and didn't make several accounts, then you can appeal this account closure:"
      )
    ),
    newAppeal("")
  )

  def page(me: User, playban: Boolean, ublogIsVisible: Boolean)(using ctx: Context) =
    val query = Granter.opt(_.Appeals).so(ctx.req.queryString.toMap)
    val isMarked =
      playban || me.marks.engine || me.marks.boost || me.marks.troll || me.marks.rankban || me.marks.arenaBan || me.marks.prizeban || !ublogIsVisible
    ui.page("Appeal a moderation decision"):
      main(cls := "page page-small box box-pad appeal force-ltr")(
        h1(cls := "box__top")("Appeal"),
        div(cls := s"nav-tree${if isMarked then " marked" else ""}")(
          if (me.enabled.no && !me.marks.boost && !me.marks.engine) || query.contains("alt")
          then altScreen
          else
            renderNode(
              {
                if me.marks.engine || query.contains("engine") then engineMenu
                else if me.marks.boost || query.contains("boost") then boostMenu
                else if me.marks.troll || query.contains("shadowban") then muteMenu
                else if playban || query.contains("playban") then playbanMenu
                else if me.marks.rankban || query.contains("rankban") then rankBanMenu
                else if me.marks.arenaBan || query.contains("arenaban") then arenaBanMenu
                else if me.marks.prizeban || query.contains("prizeban") then prizebanMenu
                else if !ublogIsVisible || query.contains("blog") then hiddenBlogMenu
                else cleanMenu
              },
              none,
              forceLtr = true
            )
        ),
        div(cls := "appeal__rules")(
          p(cls := "text warning-closure", dataIcon := Icon.CautionTriangle)(
            trans.site.closingAccountWithdrawAppeal()
          ),
          p(cls := "text", dataIcon := Icon.InfoCircle)(trans.contact.doNotMessageModerators()),
          p(
            a(cls := "text", dataIcon := Icon.InfoCircle, href := cmsPageUrl("appeal"))(
              "Read more about the appeal process"
            )
          ),
          p(a(cls := "text", dataIcon := Icon.Download, href := routes.Account.data)("Export personal data"))
        )
      )

  private val sendUsAnAppeal = frag(
    p("Send us an appeal, and a moderator will review it as soon as possible."),
    p("Add any relevant information that could help us process your appeal."),
    p("Please be honest, concise, and on point.")
  )
