"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.team)window.i18n.team={};let i=window.i18n.team;i['allTeams']="Na foirne uile";i['beingReviewed']="Tá ceannaire foirne ag athbhreithniú d’iarratas ar bheith páirteach.";i['closeTeam']="Dún foireann";i['closeTeamDescription']="Má chliceálann tú ar seo, dúnfar d’fhoireann go deo.";i['completedTourns']="Comórtais críochnaithe";i['declinedRequests']="Iarrataí Diúltaithe";i['entryCode']="Cód iontrála foirne";i['entryCodeDescriptionForLeader']="(Roghnach) Cód iontrála nach mór a bheith ar eolas ag baill nua le bheith ar an bhfoireann seo.";i['incorrectEntryCode']="Cód iontrála mícheart.";i['joinLichessVariantTeam']=s("Bí ar an bhfoireann oifigiúil %s le haghaidh nuachta agus imeachtaí");i['joinTeam']="Bí ar foireann";i['kickSomeone']="Caith duine as an bhfoireann";i['leadersChat']="Comhrá ceannairí";i['leaderTeams']="Foirne ceannaire";i['manuallyReviewAdmissionRequests']="Déan athbhreithniú de láimh ar iarratais iontrála";i['manuallyReviewAdmissionRequestsHelp']="Má dhéantar seiceáil uirthi, beidh ar ficheallaithe iarratas a scríobh chun dul isteach ar foireann, ar féidir leat a dhiúltú nó glacadh.";i['messageAllMembers']="Cuir teachtaireacht chuig gach ball";i['messageAllMembersLongDescription']="Seol teachtaireacht phríobháideach chuig GACH ball den fhoireann.\nIs féidir leat é seo a úsáid chun glaoch a chuir ar imreoirí chun páirt a ghlacadh i gcomórtas nó i gcath foirne.\nD’fhéadfadh imreoirí nach maith leo do theachtaireachtaí a fháil an fhoireann a fhágáil.";i['messageAllMembersOverview']="Seol teachtaireacht phríobháideach chuig gach ball den fhoireann";i['myTeams']="Mo fhoirne";i['nbMembers']=p({"one":"ball amháin","two":"%s bhall","few":"%s mball","many":"%s ball","other":"%s ball"});i['newTeam']="Foireann nua";i['noTeamFound']="Níor aimsíodh aon fhoireann";i['onlyLeaderLeavesTeam']="Cuir ceannaire foirne nua leis sula bhfágann tú, nó dún an fhoireann.";i['quitTeam']="Fág foireann";i['requestDeclined']="Dhiúltaigh ceannaire foirne d’iarratas ar bhallraíocht.";i['subToTeamMessages']="Liostáil le teachtaireachtaí foirne";i['swissTournamentOverview']="Comórtas Swiss nach féidir ach le baill d’fhoireann a bheith páirteach ann";i['team']="Foireann";i['teamAlreadyExists']="Tá an foireann seo ann cheana féin.";i['teamBattle']="Cath Foirne";i['teamBattleOverview']="Cath ilfhoirne, scórálann gach ficheallaí pointí dá bhfoireann";i['teamLeaders']=p({"one":"Ceannaire foirne","two":"Ceannairí foirne","few":"Ceannairí foirne","many":"Ceannairí foirne","other":"Ceannairí foirne"});i['teamRecentMembers']="Baill le déanaí";i['teams']="Foirne";i['teamsIlead']="Foirne ina bhfuil mé i gceannas";i['teamTournament']="Comórtas foirne";i['teamTournamentOverview']="Comórtas Láthair nach féidir ach baill d’fhoireann ficheallaithe a bheith páirteach ann";i['whoToKick']="Cé atá tú ag iarraidh a chaitheamh amach ón bhfoireann?";i['willBeReviewed']="Déanfaidh do ceannaire foirne athbhreithniú ar d’iarratas isteach.";i['xJoinRequests']=p({"one":"%s iarratas isteach","two":"%s iarratas isteach","few":"%s iarratas isteach","many":"%s iarratas isteach","other":"%s iarratas isteach"});i['youWayWantToLinkOneOfTheseTournaments']="Seans gur mhaith leat ceann de na comórtais seo atá le teacht suas a nascadh?"})()