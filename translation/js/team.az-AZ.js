"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.team)window.i18n.team={};let i=window.i18n.team;i['allTeams']="Bütün komandalar";i['beingReviewed']="Qoşulma istəyiniz komanda lideri tərəfindən nəzərdən keçirilir.";i['closeTeam']="Komandanı bağla";i['closeTeamDescription']="Komandanı həmişəlik bağla.";i['joinTeam']="Komandaya qoşul";i['kickSomeone']="Kimisə komandadan qov";i['leadersChat']="Liderlərin söhbəti";i['manuallyReviewAdmissionRequests']="Qəbul tələblərini əllə nəzərdən keçirin";i['manuallyReviewAdmissionRequestsHelp']="İşarələnibsə, oyunçuların komandaya qoşulmaq üçün qəbul və ya rədd edə biləcəyiniz bir tələb yazmalı olacaqlar.";i['messageAllMembers']="Bütün üzvlərə mesaj göndər";i['messageAllMembersLongDescription']="Komandanın BÜTÜN üzvlərinə özəl mesaj göndərər.\nBunu istifadə edərək oyunçuları turnirə və ya komanda döyüşünə qoşulmaq üçün çağıra bilərsiniz.\nMesajlarınızı almaq istəməyən oyunçular komandanı tərk edə bilər.";i['messageAllMembersOverview']="Komandadakı hər bir üzvünə özəl mesaj göndər";i['myTeams']="Komandalarım";i['nbMembers']=p({"one":"%s üzvləri","other":"%s üzv"});i['newTeam']="Yeni komanda";i['noTeamFound']="Komanda tapılmadı";i['onlyLeaderLeavesTeam']="Xahiş edirik, tərk etməzdən əvvəl yeni komanda lideri əlavə edin və ya komandanı silin.";i['quitTeam']="Komandanı tərk et";i['subToTeamMessages']="Komanda mesajlarına abunə olun";i['swissTournamentOverview']="Yalnız sizin komandanın üzvlərinin qoşula biləcəyi İsveçrə turniri";i['team']="Komanda";i['teamAlreadyExists']="Bu komanda artıq mövcuddur.";i['teamBattle']="Komanda Döyüşü";i['teamBattleOverview']="Çoxlu komandaların döyüşü, hər oyunçu öz komandası üçün xal qazanır";i['teamLeaders']=p({"one":"Komanda lideri","other":"Komanda liderləri"});i['teamRecentMembers']="Son qoşulan üzvlər";i['teams']="Komandalar";i['teamsIlead']="Liderlik etdiyim komandalar";i['teamTournament']="Komanda turniri";i['teamTournamentOverview']="Yalnız sizin komandanızın üzvlərinin qoşula biləcəyi Arena turniri";i['whoToKick']="Kimi komandadan qovmaq istəyirsiniz?";i['willBeReviewed']="Qoşulma istəyiniz komanda lideri tərəfindən nəzərdən keçiriləcək.";i['xJoinRequests']=p({"one":"%s qoşulma istəyi","other":"%s qoşulma tələbi"});i['youWayWantToLinkOneOfTheseTournaments']="Yaxınlaşan turnirlərdən birinə qoşulmaq istəyirsiniz?"})()