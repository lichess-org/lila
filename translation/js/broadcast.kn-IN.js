"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.broadcast)window.i18n.broadcast={};let i=window.i18n.broadcast;i['addRound']="ಒಂದು ಸುತ್ತನ್ನು ಸೇರಿಸಿ";i['broadcasts']="ಪ್ರಸಾರಗಳು";i['completed']="ಪೂರ್ಣಗೊಂಡಿದೆ";i['credits']="ಮೂಲವನ್ನು ಕ್ರೆಡಿಟ್ ಮಾಡಿ";i['currentGameUrl']="ಪ್ರಸ್ತುತ ಆಟದ URL";i['definitivelyDeleteRound']="ರೌಂಡ್ ಮತ್ತು ಅದರ ಎಲ್ಲಾ ಆಟಗಳನ್ನು ಖಚಿತವಾಗಿ ಅಳಿಸಿ.";i['definitivelyDeleteTournament']="ಸಂಪೂರ್ಣ ಪಂದ್ಯಾವಳಿ, ಅದರ ಎಲ್ಲಾ ಸುತ್ತುಗಳು ಮತ್ತು ಅದರ ಎಲ್ಲಾ ಆಟಗಳನ್ನು ಖಚಿತವಾಗಿ ಅಳಿಸಿ.";i['deleteAllGamesOfThisRound']="ಈ ಸುತ್ತಿನ ಎಲ್ಲಾ ಆಟಗಳನ್ನು ಅಳಿಸಿ. ಅವುಗಳನ್ನು ಮರು-ರಚಿಸಲು ಮೂಲವು ಸಕ್ರಿಯವಾಗಿರಬೇಕು.";i['deleteRound']="ಈ ಸುತ್ತನ್ನು ಅಳಿಸಿ";i['deleteTournament']="ಈ ಪಂದ್ಯಾವಳಿಯನ್ನು ಅಳಿಸಿ";i['downloadAllRounds']="ಎಲ್ಲಾ ಸುತ್ತುಗಳನ್ನು ಡೌನ್‌ಲೋಡ್ ಮಾಡಿ";i['editRoundStudy']="ಸುತ್ತಿನ ಅಧ್ಯಯನವನ್ನು ಸಂಪಾದಿಸಿ";i['fullDescription']="ಪಂದ್ಯಾವಳಿಯ ಸಂಪೂರ್ಣ ವಿವರಣೆ";i['fullDescriptionHelp']=s("ಪಂದ್ಯಾವಳಿಯ ಐಚ್ಛಿಕ ದೀರ್ಘ ವಿವರಣೆ. %1$s ಲಭ್ಯವಿದೆ. ಉದ್ದವು %2$s ಅಕ್ಷರಗಳಿಗಿಂತ ಕಡಿಮೆಯಿರಬೇಕು.");i['liveBroadcasts']="ಪಂದ್ಯಾವಳಿಯ ನೇರ ಪ್ರಸಾರ";i['nbBroadcasts']=p({"one":"%s ಪ್ರಸಾರ","other":"%s ಪ್ರಸಾರಗಳು"});i['newBroadcast']="ಹೊಸ ನೇರ ಪ್ರಸಾರ";i['ongoing']="ಚಾಲ್ತಿಯಲ್ಲಿದೆ";i['resetRound']="ಈ ಸುತ್ತನ್ನು ಮರುಹೊಂದಿಸಿ";i['roundName']="ಸುತ್ತಿನ ಹೆಸರು";i['roundNumber']="ಸುತ್ತಿನ ಸಂಖ್ಯೆ";i['sourceUrlHelp']="PGN ನವೀಕರಣಗಳನ್ನು ಪಡೆಯಲು Lichess ಪರಿಶೀಲಿಸುವ URL. ಇದನ್ನು ಇಂಟರ್ನೆಟ್‌ನಿಂದ ಸಾರ್ವಜನಿಕವಾಗಿ ಪ್ರವೇಶಿಸಬೇಕು.";i['startDateHelp']="ಐಚ್ಛಿಕ, ಈವೆಂಟ್ ಯಾವಾಗ ಪ್ರಾರಂಭವಾಗುತ್ತದೆ ಎಂದು ನಿಮಗೆ ತಿಳಿದಿದ್ದರೆ";i['tournamentDescription']="ಸಣ್ಣ ಪಂದ್ಯಾವಳಿಯ ವಿವರಣೆ";i['tournamentName']="ಪಂದ್ಯಾವಳಿಯ ಹೆಸರು";i['upcoming']="ಮುಂಬರುವ"})()