"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.team)window.i18n.team={};let i=window.i18n.team;i['allTeams']="ಎಲ್ಲಾ ತಂಡಗಳು";i['battleOfNbTeams']=p({"one":"%s ತಂಡಗಳ ಕದನ","other":"%s ತಂಡಗಳ ಕದನ"});i['beingReviewed']="ನಿಮ್ಮ ಸೇರ್ಪಡೆ ವಿನಂತಿಯನ್ನು ತಂಡದ ನಾಯಕರೊಬ್ಬರು ಪರಿಶೀಲಿಸುತ್ತಿದ್ದಾರೆ.";i['closeTeam']="ತಂಡವನ್ನು ಮುಚ್ಚಿ";i['closeTeamDescription']="ತಂಡವನ್ನು ಶಾಶ್ವತವಾಗಿ ಮುಚ್ಚುತ್ತದೆ.";i['completedTourns']="ಪಂದ್ಯಾವಳಿಗಳನ್ನು ಪೂರ್ಣಗೊಳಿಸಿದೆ";i['declinedRequests']="ನಿರಾಕರಿಸಿದ ವಿನಂತಿಗಳು";i['entryCode']="ತಂಡದ ಪ್ರವೇಶ ಕೋಡ್";i['entryCodeDescriptionForLeader']="(ಐಚ್ಛಿಕ) ಈ ತಂಡವನ್ನು ಸೇರಲು ಹೊಸ ಸದಸ್ಯರು ತಿಳಿದಿರಬೇಕಾದ ಪ್ರವೇಶ ಕೋಡ್.";i['incorrectEntryCode']="ತಪ್ಪಾದ ಪ್ರವೇಶ ಕೋಡ್.";i['innerTeam']="ಒಳ ತಂಡ";i['joinLichessVariantTeam']=s("ಸುದ್ದಿ ಮತ್ತು ಈವೆಂಟ್‌ಗಳಿಗಾಗಿ ಅಧಿಕೃತ %s ತಂಡವನ್ನು ಸೇರಿ");i['joinTeam']="ತಂಡವನ್ನು ಸೇರಿ";i['kickSomeone']="ಯಾರನ್ನಾದರೂ ತಂಡದಿಂದ ಹೊರಹಾಕಿ";i['leadersChat']="ನಾಯಕರು ಮಾತುಕತೆ";i['leaderTeams']="ನಾಯಕ ತಂಡಗಳು";i['listTheTeamsThatWillCompete']="ಈ ಯುದ್ಧದಲ್ಲಿ ಸ್ಪರ್ಧಿಸುವ ತಂಡಗಳನ್ನು ಪಟ್ಟಿ ಮಾಡಿ.";i['manuallyReviewAdmissionRequests']="ಪ್ರವೇಶ ವಿನಂತಿಗಳನ್ನು ಹಸ್ತಚಾಲಿತವಾಗಿ ಪರಿಶೀಲಿಸಿ";i['manuallyReviewAdmissionRequestsHelp']="ಪರಿಶೀಲಿಸಿದರೆ, ಆಟಗಾರರು ತಂಡಕ್ಕೆ ಸೇರಲು ವಿನಂತಿಯನ್ನು ಬರೆಯಬೇಕಾಗುತ್ತದೆ, ಅದನ್ನು ನೀವು ನಿರಾಕರಿಸಬಹುದು ಅಥವಾ ಸ್ವೀಕರಿಸಬಹುದು.";i['messageAllMembers']="ಎಲ್ಲಾ ಸದಸ್ಯರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ";i['messageAllMembersLongDescription']="ತಂಡದ ಎಲ್ಲಾ ಸದಸ್ಯರಿಗೆ ಖಾಸಗಿ ಸಂದೇಶವನ್ನು ಕಳುಹಿಸಿ.\nಪಂದ್ಯಾವಳಿ ಅಥವಾ ತಂಡದ ಯುದ್ಧಕ್ಕೆ ಸೇರಲು ಆಟಗಾರರನ್ನು ಕರೆಯಲು ನೀವು ಇದನ್ನು ಬಳಸಬಹುದು.\nನಿಮ್ಮ ಸಂದೇಶಗಳನ್ನು ಸ್ವೀಕರಿಸಲು ಇಷ್ಟಪಡದ ಆಟಗಾರರು ತಂಡವನ್ನು ತೊರೆಯಬಹುದು.";i['messageAllMembersOverview']="ತಂಡದ ಪ್ರತಿಯೊಬ್ಬ ಸದಸ್ಯರಿಗೂ ಖಾಸಗಿ ಸಂದೇಶವನ್ನು ಕಳುಹಿಸಿ";i['myTeams']="ನನ್ನ ತಂಡಗಳು";i['nbMembers']=p({"one":"%s ಸದಸ್ಯರು","other":"%s ಸದಸ್ಯರು"});i['newTeam']="ಹೊಸ ತಂಡ";i['noTeamFound']="ಯಾವುದೇ ತಂಡ ಕಂಡುಬಂದಿಲ್ಲ";i['numberOfLeadsPerTeam']="ಪ್ರತಿ ತಂಡಕ್ಕೆ ನಾಯಕರ ಸಂಖ್ಯೆ. ಅವರ ಸ್ಕೋರ್ ಮೊತ್ತವು ತಂಡದ ಸ್ಕೋರ್ ಆಗಿದೆ.";i['numberOfLeadsPerTeamHelp']="ಪಂದ್ಯಾವಳಿ ಪ್ರಾರಂಭವಾದ ನಂತರ ನೀವು ನಿಜವಾಗಿಯೂ ಈ ಮೌಲ್ಯವನ್ನು ಬದಲಾಯಿಸಬಾರದು!";i['oneTeamPerLine']="ಪ್ರತಿ ಸಾಲಿಗೆ ಒಂದು ತಂಡ. ಸ್ವಯಂ ಪೂರ್ಣಗೊಳಿಸುವಿಕೆಯನ್ನು ಬಳಸಿ.";i['oneTeamPerLineHelp']="ನೀವು ಈ ಪಟ್ಟಿಯನ್ನು ಪಂದ್ಯಾವಳಿಯಿಂದ ಇನ್ನೊಂದಕ್ಕೆ ನಕಲಿಸಬಹುದು!\n\nಆಟಗಾರನು ಈಗಾಗಲೇ ಅದರೊಂದಿಗೆ ಪಂದ್ಯಾವಳಿಗೆ ಸೇರಿಕೊಂಡಿದ್ದರೆ ನೀವು ತಂಡವನ್ನು ತೆಗೆದುಹಾಕಲು ಸಾಧ್ಯವಿಲ್ಲ.";i['onlyLeaderLeavesTeam']="ದಯವಿಟ್ಟು ಹೊರಡುವ ಮೊದಲು ಹೊಸ ತಂಡದ ನಾಯಕನನ್ನು ಸೇರಿಸಿ ಅಥವಾ ತಂಡವನ್ನು ಮುಚ್ಚಿ.";i['quitTeam']="ತಂಡವನ್ನು ತೊರೆಯಿರಿ";i['requestDeclined']="ನಿಮ್ಮ ಸೇರ್ಪಡೆ ವಿನಂತಿಯನ್ನು ತಂಡದ ನಾಯಕರೊಬ್ಬರು ನಿರಾಕರಿಸಿದ್ದಾರೆ.";i['subToTeamMessages']="ತಂಡದ ಸಂದೇಶಗಳಿಗೆ ಚಂದಾದಾರರಾಗಿ";i['swissTournamentOverview']="ನಿಮ್ಮ ತಂಡದ ಸದಸ್ಯರು ಮಾತ್ರ ಸೇರಬಹುದಾದ ಸ್ವಿಸ್ ಪಂದ್ಯಾವಳಿ";i['team']="ತಂಡ";i['teamAlreadyExists']="ಈ ತಂಡ ಈಗಾಗಲೇ ಅಸ್ತಿತ್ವದಲ್ಲಿದೆ.";i['teamBattle']="ತಂಡದ ಯುದ್ಧ";i['teamBattleOverview']="ಬಹು ತಂಡಗಳ ಯುದ್ಧ, ಪ್ರತಿ ಆಟಗಾರನು ತನ್ನ ತಂಡಕ್ಕೆ ಅಂಕಗಳನ್ನು ಗಳಿಸುತ್ತಾನೆ";i['teamLeaders']=p({"one":"ತಂಡದ ನಾಯಕ","other":"ತಂಡದ ನಾಯಕರು"});i['teamPage']="ತಂಡದ ಪುಟ";i['teamRecentMembers']="ಇತ್ತೀಚಿನ ಸದಸ್ಯರು";i['teams']="ತಂಡಗಳು";i['teamsIlead']="ನಾನು ಮುನ್ನಡೆಸುವ ತಂಡಗಳು";i['teamTournament']="ತಂಡದ ಪಂದ್ಯಾವಳಿ";i['teamTournamentOverview']="ನಿಮ್ಮ ತಂಡದ ಸದಸ್ಯರು ಮಾತ್ರ ಸೇರಬಹುದಾದ ಅರೆನಾ ಪಂದ್ಯಾವಳಿ";i['thisTeamBattleIsOver']="ಈ ಪಂದ್ಯಾವಳಿ ಮುಗಿದಿದೆ ಮತ್ತು ತಂಡಗಳನ್ನು ಇನ್ನು ಮುಂದೆ ನವೀಕರಿಸಲಾಗುವುದಿಲ್ಲ.";i['upcomingTournaments']="ಮುಂಬರುವ ಪಂದ್ಯಾವಳಿಗಳು";i['whoToKick']="ನೀವು ಯಾರನ್ನು ತಂಡದಿಂದ ಹೊರಹಾಕಲು ಬಯಸುತ್ತೀರಿ?";i['willBeReviewed']="ನಿಮ್ಮ ಸೇರ್ಪಡೆ ವಿನಂತಿಯನ್ನು ತಂಡದ ನಾಯಕರೊಬ್ಬರು ಪರಿಶೀಲಿಸುತ್ತಾರೆ.";i['xJoinRequests']=p({"one":"%s ಸೇರಲು ವಿನಂತಿ","other":"%s ಸೇರಲು ವಿನಂತಿಗಳು"});i['youWayWantToLinkOneOfTheseTournaments']="ಈ ಮುಂಬರುವ ಪಂದ್ಯಾವಳಿಗಳಲ್ಲಿ ಒಂದನ್ನು ನೀವು ಲಿಂಕ್ ಮಾಡಲು ಬಯಸಬಹುದು?"})()