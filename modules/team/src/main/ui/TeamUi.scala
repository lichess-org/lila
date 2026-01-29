package lila.team
package ui

import scalalib.paginator.Paginator

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class TeamUi(helpers: Helpers, markdownCache: lila.memo.MarkdownCache):
  import helpers.{ *, given }
  import trans.team as trt

  def TeamPage(title: String) = Page(title).css("bits.team").js(infiniteScrollEsmInit)

  object markdown:
    private val options = lila.memo.MarkdownOptions(
      autoLink = true,
      header = true,
      list = true,
      table = true,
      maxPgns = Max(0)
    )

    def apply(team: Team, text: Markdown): Frag =
      markdownCache.toHtmlSyncWithoutPgnEmbeds(s"team:${team.id}", text, options)

  def menu(currentTab: Option[String])(using ctx: Context) =
    val tab = ~currentTab
    st.nav(cls := "page-menu__menu subnav")(
      tab
        .contains("requests")
        .option:
          a(cls := tab.active("requests"), href := routes.Team.requests)("Join requests")
      ,
      ctx.isAuth.option(
        a(cls := tab.active("mine"), href := routes.Team.mine)(trt.myTeams())
      ),
      ctx.isAuth.option(
        a(cls := tab.active("leader"), href := routes.Team.leader)(trt.leaderTeams())
      ),
      a(cls := tab.active("all"), href := routes.Team.all())(trt.allTeams()),
      ctx.isAuth.option(
        a(cls := tab.active("form"), href := routes.Team.form)(trt.newTeam())
      )
    )

  def teamTr(t: Team.WithMyLeadership)(using Context) =
    val isMine = isMyTeamSync(t.id)
    tr(cls := "paginated")(
      td(cls := "subject")(
        a(
          dataIcon := Icon.Group,
          cls := List(
            "team-name text" -> true,
            "mine" -> isMine
          ),
          href := routes.Team.show(t.id)
        )(
          t.name,
          t.flair.map(teamFlair),
          t.amLeader.option(em("leader"))
        ),
        ~t.intro: String
      ),
      td(cls := "info")(
        p(trt.nbMembers.plural(t.nbMembers, t.nbMembers.localize)),
        isMine.option:
          st.form(action := routes.Team.quit(t.id), method := "post")(
            submitButton(cls := "button button-empty button-red button-thin yes-no-confirm team__quit")(
              trt.quitTeam.txt()
            )
          )
      )
    )

  def membersPage(t: Team, pager: Paginator[TeamMember.UserAndDate])(using Context) =
    TeamPage(t.name).graph(
      title = s"${t.name} • ${trt.teamRecentMembers.txt()}",
      url = routeUrl(routes.Team.show(t.id)),
      description = t.intro.so(shorten(_, 152))
    ):
      main(cls := "page-small box")(
        boxTop(
          h1(
            teamLink(t.light, true),
            " • ",
            trt.nbMembers.plural(t.nbMembers, t.nbMembers.localize)
          )
        ),
        table(cls := "team-members slist slist-pad slist-invert"):
          tbody(cls := "infinite-scroll")(
            pager.currentPageResults.map { case TeamMember.UserAndDate(u, date) =>
              tr(cls := "paginated")(
                td(lightUserLink(u)),
                td(momentFromNowOnce(date))
              )
            },
            pagerNextTable(pager, np => routes.Team.members(t.slug, np).url)
          )
      )

  object list:

    def search(text: String, teams: Paginator[Team.WithMyLeadership])(using Context) =
      list(
        name = s"""${trans.search.search.txt()} "$text"""",
        teams = teams,
        nextPageUrl = n => routes.Team.search(text, n).url,
        search = text
      )

    def all(teams: Paginator[Team.WithMyLeadership])(using Context) =
      list(
        name = trt.teams.txt(),
        teams = teams,
        nextPageUrl = n => routes.Team.all(n).url
      )

    def mine(teams: List[Team.WithMyLeadership])(using ctx: Context) =
      TeamPage(trt.myTeams.txt()):
        main(cls := "team-list page-menu")(
          menu("mine".some),
          div(cls := "page-menu__content box")(
            h1(cls := "box__top")(trt.myTeams()),
            standardFlash.map(div(cls := "box__pad")(_)),
            ctx.me.filter(me => Team.maxJoin(me) < teams.size).map { me =>
              flashMessage("failure"):
                s"You have joined ${teams.size} out of ${Team.maxJoin(me)} teams. Leave some teams before you can join others."
            },
            table(cls := "slist slist-pad slist-invert")(
              if teams.nonEmpty then tbody(teams.map(teamTr(_)))
              else noTeam()
            )
          )
        )

    def ledByMe(teams: List[Team])(using Context) =
      TeamPage(trt.myTeams.txt()):
        main(cls := "team-list page-menu")(
          menu("leader".some),
          div(cls := "page-menu__content box")(
            h1(cls := "box__top")(trt.teamsIlead()),
            standardFlash,
            table(cls := "slist slist-pad slist-invert")(
              if teams.nonEmpty then tbody(teams.map(Team.WithMyLeadership(_, true)).map(teamTr(_)))
              else noTeam()
            )
          )
        )

    private def list(
        name: String,
        teams: Paginator[Team.WithMyLeadership],
        nextPageUrl: Int => String,
        search: String = ""
    )(using Context) =
      TeamPage(s"$name - page ${teams.currentPage}"):
        main(cls := "team-list page-menu")(
          menu("all".some),
          div(cls := "page-menu__content box")(
            boxTop(
              h1(name),
              div(cls := "box__top__actions")(
                st.form(cls := "search", action := routes.Team.search())(
                  input(st.name := "text", value := search, placeholder := trans.search.search.txt())
                )
              )
            ),
            standardFlash,
            table(cls := "slist slist-pad slist-invert")(
              if teams.nbResults > 0 then
                tbody(cls := "infinite-scroll")(
                  teams.currentPageResults.map(teamTr),
                  pagerNextTable(teams, nextPageUrl)
                )
              else noTeam()
            )
          )
        )

    private def noTeam()(using Context) =
      tbody(tr(td(colspan := "2")(br, trt.noTeamFound())))

  def members(team: Team, members: Paginator[lila.core.LightUser])(using Translate) =
    div(cls := "team-show__members")(
      st.section(cls := "recent-members")(
        h2(a(href := routes.Team.members(team.slug))(trt.teamRecentMembers())),
        div(cls := "userlist infinite-scroll")(
          members.currentPageResults.map: member =>
            div(cls := "paginated")(lightUserLink(member)),
          pagerNext(members, np => routes.Team.show(team.id, np).url)
        )
      )
    )

  def actions(
      team: Team,
      member: Option[TeamMember],
      myRequest: Option[TeamRequest],
      subscribed: Boolean,
      asMod: Boolean
  )(using ctx: Context) =
    def hasPerm(perm: TeamSecurity.Permission.Selector) = member.exists(_.hasPerm(perm))
    val canManage = asMod && Granter.opt(_.ManageTeam)
    div(cls := "team-show__actions")(
      (team.enabled && team.acceptsMembers && member.isEmpty).option(
        frag(
          if myRequest.exists(_.declined) then
            frag(
              strong(trt.requestDeclined()),
              a(cls := "button disabled button-metal")(trt.joinTeam())
            )
          else if myRequest.isDefined then
            frag(
              strong(trt.beingReviewed()),
              postForm(action := routes.Team.quit(team.id)):
                submitButton(cls := "button button-red button-empty yes-no-confirm")(trans.site.cancel())
            )
          else (ctx.isAuth && !asMod).option(joinButton(team))
        )
      ),
      (team.enabled && team.doesTeamMessages && member.isDefined).option(
        postForm(
          cls := "team-show__subscribe form3",
          action := routes.Team.subscribe(team.id)
        )(
          div(
            span(form3.cmnToggle("team-subscribe", "subscribe", checked = subscribed)),
            label(`for` := "team-subscribe")(trt.subToTeamMessages.txt())
          )
        )
      ),
      (member.isDefined && !team.isClas && !hasPerm(_.Admin)).option(
        postForm(cls := "quit", action := routes.Team.quit(team.id))(
          submitButton(cls := "button button-empty button-red yes-no-confirm")(trt.quitTeam.txt())
        )
      ),
      (team.enabled && hasPerm(_.Tour)).option(
        frag(
          team.isClas.not.option:
            a(
              href := routes.Tournament.teamBattleForm(team.id),
              cls := "button button-empty text",
              dataIcon := Icon.Trophy
            ):
              span(
                strong(trt.teamBattle()),
                em(trt.teamBattleOverview())
              )
          ,
          a(
            href := addQueryParams(
              routes.Tournament.form.url,
              Map("team" -> team.id.value) ++ team.isClas.so(Map("clas" -> "1"))
            ),
            cls := "button button-empty text",
            dataIcon := Icon.Trophy
          ):
            span(
              strong(trt.teamTournament()),
              em(trt.teamTournamentOverview())
            )
          ,
          a(
            href := s"${routes.Swiss.form(team.id)}",
            cls := "button button-empty text",
            dataIcon := Icon.Trophy
          ):
            span(
              strong(trans.swiss.swissTournaments()),
              em(trt.swissTournamentOverview())
            )
        )
      ),
      (team.enabled && hasPerm(_.PmAll)).option(
        frag(
          a(
            href := routes.Team.pmAll(team.id),
            cls := "button button-empty text",
            dataIcon := Icon.Envelope
          ):
            span(
              strong(trt.messageAllMembers()),
              em(trt.messageAllMembersOverview())
            )
        )
      ),
      ((team.enabled && hasPerm(_.Settings)) || canManage).option(
        a(
          href := routes.Team.edit(team.id),
          cls := "button button-empty text",
          dataIcon := Icon.Gear
        )(
          trans.settings.settings()
        )
      ),
      ((team.enabled && hasPerm(_.Admin)) || canManage).option(
        a(
          cls := "button button-empty text",
          href := routes.Team.leaders(team.id),
          dataIcon := Icon.Group
        )(trt.teamLeaders())
      ),
      ((team.enabled && hasPerm(_.Kick)) || canManage).option(
        a(
          cls := "button button-empty text",
          href := routes.Team.kick(team.id),
          dataIcon := Icon.InternalArrow
        )(trt.kickSomeone())
      ),
      ((team.enabled && hasPerm(_.Request)) || canManage).option(
        a(
          cls := "button button-empty text",
          href := routes.Team.declinedRequests(team.id),
          dataIcon := Icon.Cancel
        )(trt.declinedRequests())
      ),
      ((Granter.opt(_.ManageTeam) || Granter.opt(_.Shusher)) && !asMod).option(
        a(
          href := routes.Team.show(team.id, 1, mod = true),
          cls := "button button-red"
        ):
          "View team as Mod"
      )
    )

  // handle special teams here
  private def joinButton(t: Team)(using Context) =
    t.id.value match
      case "english-chess-players" => joinAt("https://ecf.octoknight.com/")
      case "ecf" => joinAt(routes.Team.show(TeamId("english-chess-players")).url)
      case _ =>
        postForm(cls := "inline", action := routes.Team.join(t.id))(
          submitButton(cls := "button button-green")(trt.joinTeam())
        )

  private def joinAt(url: String)(using Context) =
    a(cls := "button button-green", href := url)(trt.joinTeam())
