"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.team)window.i18n.team={};let i=window.i18n.team;i['allTeams']="Барлык төркемнәр";i['beingReviewed']="Сезнең кушылу соравыгызны төркем җитәкчесе карап чыга.";i['closeTeam']="Җәмәгане ябу";i['closeTeamDescription']="Җәмәгәне ябырга мәңгегә.";i['joinTeam']="Җәмәгагә кушылу";i['kickSomeone']="Җәмәгадән куып чыгарырга";i['leadersChat']="Рәһбәләр сөхбәтләшмәктә";i['manuallyReviewAdmissionRequests']="Кабул итү гаризаларын кул белән карагыз";i['manuallyReviewAdmissionRequestsHelp']="Тикшерелсә, уенчыларга командага кушылу турында гариза язарга кирәк, сез аны кире кагарга яки кабул итә аласыз.";i['messageAllMembers']="Бөтен уенчыларына хәбәр";i['messageAllMembersLongDescription']="Төркемнең һәр уенчына шәхси хәбәр җибәрү.\nСез моны уенчыларны турнирга я төркемнәр ярышына чакыру өчен итә аласыз.\nСездән хәбәр алырга теләмәгән уенчылар төркемнән чыга ала.";i['messageAllMembersOverview']="Җәмәганең һәр уенчына шәхси хәбәр җибәрү";i['myTeams']="Җәмәгаләрем";i['nbMembers']=p({"other":"%s әгъза"});i['newTeam']="Яңа төркем";i['noTeamFound']="Бер җәмәга дә табылмады";i['quitTeam']="Төркемнән чыгу";i['team']="Җәмәга";i['teamAlreadyExists']="Бу җәмәга инде бар.";i['teamBattle']="Төркемнәр ярышы";i['teamBattleOverview']="Төрле төркемнәр ярышы, һәр уенче үзенең төркеме өчен баллар җыя";i['teamLeaders']=p({"other":"Төркем җитәкчеләре"});i['teamRecentMembers']="Яңа кушылган уенчылар";i['teams']="Төркемнәр";i['teamsIlead']="Җәмәга рәһбәсе";i['teamTournament']="Төркем турниры";i['teamTournamentOverview']="Сезнең төркем уенчылары гына кушыла алган Арена турниры";i['whoToKick']="Кемне куып чыгарырга телисез җәмәгадән?";i['willBeReviewed']="Сезнең кушылу соравыгызны төркем җитәкчесе карап чыгачак.";i['xJoinRequests']=p({"other":"%s кушылу соравы"});i['youWayWantToLinkOneOfTheseTournaments']="Сез киләсе бәйгеләрнең берсен бәйләргә телисездер?"})()