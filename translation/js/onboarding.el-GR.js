"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.onboarding)window.i18n.onboarding={};let i=window.i18n.onboarding;i['configureLichess']="Ρυθμίστε το Lichess σύμφωνα με τις προτιμήσεις σας.";i['enabledKidModeSuggestion']=s("Θα χρησιμοποιηθεί ο λογαριασμός από ένα παιδί; Ίσως να θέλετε να ενεργοποιήσετε το %s.");i['exploreTheSiteAndHaveFun']="Εξερευνήστε την ιστοσελίδα και διασκεδάστε :)";i['followYourFriendsOnLichess']="Ακολουθήστε τους φίλους σας στο Lichess.";i['improveWithChessTacticsPuzzles']="Βελτιωθείτε με σκακιστικά παζλ τακτικής.";i['learnChessRules']="Μάθετε τους σκακιστικούς κανονισμούς";i['learnFromXAndY']=s("Μάθετε από %1$s και %2$s.");i['playInTournaments']="Παίξτε σε τουρνουά.";i['playOpponentsFromAroundTheWorld']="Παίξτε με αντιπάλους από όλο τον κόσμο.";i['playTheArtificialIntelligence']="Πέξτε με αντίπαλο την τεχνητή νοημοσύνη.";i['thisIsYourProfilePage']="Αυτή είναι η σελίδα του προφίλ σας.";i['welcome']="Καλώς ήρθατε!";i['welcomeToLichess']="Καλώς ήρθατε στο lichess.org!";i['whatNowSuggestions']="Και τώρα τι; Εδώ είναι μερικές προτάσεις:"})()