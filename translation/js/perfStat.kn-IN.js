"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.perfStat)window.i18n.perfStat={};let i=window.i18n.perfStat;i['averageOpponent']="ಸರಾಸರಿ ಎದುರಾಳಿ";i['berserkedGames']="ಬೆರ್ಸರ್ಕ್ಡ್ ಆಟಗಳು";i['bestRated']="ಅತ್ಯುತ್ತಮ ರೇಟ್ ಮಾಡಿದ ವಿಜಯಗಳು";i['currentStreak']=s("ಪ್ರಸ್ತುತ ಸರಣಿ: %s");i['defeats']="ಸೋಲುಗಳು";i['disconnections']="ಸಂಪರ್ಕ ಕಡಿತಗಳು";i['fromXToY']=s("%1$s ರಿಂದ %2$s ವರೆಗೆ");i['gamesInARow']="ಆಟಗಳನ್ನು ಸತತವಾಗಿ ಆಡಲಾಗುತ್ತದೆ";i['highestRating']=s("ಅತ್ಯಧಿಕ ರೇಟಿಂಗ್: %s");i['lessThanOneHour']="ಆಟಗಳ ನಡುವೆ ಒಂದು ಗಂಟೆಗಿಂತ ಕಡಿಮೆ";i['longestStreak']=s("ಅತಿ ಉದ್ದದ ಸ್ಟ್ರೀಕ್ ಗೆಲುವುಗಳು: %s");i['losingStreak']="ಸೋತ ಸರಣಿ";i['lowestRating']=s("ಕಡಿಮೆ ರೇಟಿಂಗ್: %s");i['maxTimePlaying']="ಆಟವಾಡಿದ ಗರಿಷ್ಠ ಸಮಯ";i['notEnoughGames']="ಸಾಕಷ್ಟು ಆಟಗಳನ್ನು ಆಡಿಲ್ಲ";i['notEnoughRatedGames']="ವಿಶ್ವಾಸಾರ್ಹ ರೇಟಿಂಗ್ ಅನ್ನು ಸ್ಥಾಪಿಸಲು ಸಾಕಷ್ಟು ರೇಟ್ ಮಾಡಲಾದ ಆಟಗಳನ್ನು ಆಡಲಾಗಿಲ್ಲ.";i['now']="ಈಗ";i['perfStats']=s("%s ಅಂಕಿಅಂಶಗಳು");i['progressOverLastXGames']=s("ಕಳೆದ %s ಆಟಗಳಲ್ಲಿ ಪ್ರಗತಿ:");i['provisional']="ತಾತ್ಕಾಲಿಕ";i['ratedGames']="ರೇಟ್ ಮಾಡಿದ ಆಟಗಳು";i['ratingDeviation']=s("ರೇಟಿಂಗ್ ವಿಚಲನ: %s.");i['ratingDeviationTooltip']=s("ಕಡಿಮೆ ಮೌಲ್ಯ ಎಂದರೆ ರೇಟಿಂಗ್ ಹೆಚ್ಚು ಸ್ಥಿರವಾಗಿರುತ್ತದೆ. %1$s ಮೇಲೆ, ರೇಟಿಂಗ್ ಅನ್ನು ತಾತ್ಕಾಲಿಕವೆಂದು ಪರಿಗಣಿಸಲಾಗುತ್ತದೆ. ಶ್ರೇಯಾಂಕಗಳಲ್ಲಿ ಸೇರಿಸಲು, ಈ ಮೌಲ್ಯವು %2$s (ಸ್ಟ್ಯಾಂಡರ್ಡ್ ಚೆಸ್) ಅಥವಾ %3$s (ರೂಪಾಂತರಗಳು) ಗಿಂತ ಕೆಳಗಿರಬೇಕು.");i['timeSpentPlaying']="ಆಟವಾಡುತ್ತಿದ್ದ ಸಮಯ";i['totalGames']="ಒಟ್ಟು ಆಟಗಳು";i['tournamentGames']="ಟೂರ್ನಮೆಂಟ್ ಆಟಗಳು";i['victories']="ವಿಜಯಗಳು";i['viewTheGames']="ಆಟಗಳನ್ನು ವೀಕ್ಷಿಸಿ";i['winningStreak']="ಗೆಲುವಿನ ಸರಮಾಲೆ"})()