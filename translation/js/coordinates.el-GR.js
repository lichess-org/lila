"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.coordinates)window.i18n.coordinates={};let i=window.i18n.coordinates;i['aCoordinateAppears']="Μια συντεταγμένη εμφανίζεται στη σκακιέρα και πρέπει να κάνετε κλικ στο αντίστοιχο τετράγωνο.";i['aSquareIsHighlightedExplanation']="Ένα τετράγωνο επισημαίνεται στη σκακιέρα και πρέπει να εισάγετε τη συντεταγμένη του (π.χ. \\\"e4\\\").";i['averageScoreAsBlackX']=s("Μέση βαθμολογία ως μαύρα: %s");i['averageScoreAsWhiteX']=s("Μέση βαθμολογία ως λευκά: %s");i['coordinates']="Συντεταγμένες";i['coordinateTraining']="Εξάσκηση στις συντεταγμένες";i['findSquare']="Εύρεση τετραγώνου";i['goAsLongAsYouWant']="Πάρτε όσο χρόνο θέλετε, δεν υπάρχει χρονικό όριο!";i['knowingTheChessBoard']="Η γνώση των συντεταγμένων της σκακιέρας είναι μια πολύ σημαντική δεξιότητα:";i['mostChessCourses']="Τα περισσότερα μαθήματα κι οι περισσότερες ασκήσεις στο σκάκι χρησιμοποιούν τον αλγεβρικό τρόπο γραφής.";i['nameSquare']="Ονομασία τετραγώνου";i['showCoordinates']="Εμφάνιση συντεταγμένων";i['showCoordsOnAllSquares']="Συντεταγμένες σε κάθε τετράγωνο";i['showPieces']="Εμφάνιση κομματιών";i['startTraining']="Έναρξη εξάσκησης";i['talkToYourChessFriends']="Καθίσταται ευκολότερο να επικοινωνήσετε με τους φίλους σας στο σκάκι, δεδομένου ότι και οι δύο καταλαβαίνετε τη γλώσσα του σκακιού.";i['youCanAnalyseAGameMoreEffectively']="Μπορείτε να αναλύσετε ένα παιχνίδι αποτελεσματικότερα εάν δεν χρειάζεται να αναζητάτε τα ονόματα των τετραγώνων.";i['youHaveThirtySeconds']="Έχετε 30 δευτερόλεπτα για να διαλέξτε σωστά όσα τετράγωνα γίνεται!"})()