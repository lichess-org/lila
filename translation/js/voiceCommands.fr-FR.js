"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.voiceCommands)window.i18n.voiceCommands={};let i=window.i18n.voiceCommands;i['cancelTimerOrDenyARequest']="Annuler la minuterie ou refuser une demande";i['castle']="Roquer (petit ou grand roque)";i['instructions1']=s("Utilisez le bouton %1$s pour activer/désactiver la reconnaissance vocale, le bouton %2$s pour ouvrir cette fenêtre d\\'aide et le menu %3$s pour modifier les paramètres vocaux.");i['instructions2']="Nous montrons des flèches pour plusieurs coups en cas d\\'incertitude. Dites la couleur ou le numéro d\\'une flèche pour la sélectionner.";i['instructions3']=s("Si une flèche montre une radar actif, le coup sera joué lorsque le radar deviendra inactif. Durant ce temps, vous pouvez seulement dire %1$s pour jouer le coup immédiatement, %2$s pour l\\'annuler, ou dire la couleur/le numéro d\\'une flèche différente. Ce minuteur peut être réglé ou désactivé dans les paramètres.");i['instructions4']=s("Activer %s dans un environnement bruyant. Maintenir la touche Maj enfoncée pour dire une commande pendant que cette fonction est activée.");i['instructions5']="Utilisez l\\'alphabet phonétique pour améliorer la reconnaissance des colonnes de l\\'échiquier.";i['instructions6']=s("%s explique en détail les paramètres des commandes de déplacement vocales.");i['moveToE4OrSelectE4Piece']="Déplacer à e4 ou sélectionner une pièce à e4";i['phoneticAlphabetIsBest']="L\\'alphabet phonétique est l\\'idéal";i['playPreferredMoveOrConfirmSomething']="Jouer le coup préféré ou confirmer quelque chose";i['selectOrCaptureABishop']="Sélectionner ou capturer un fou";i['showPuzzleSolution']="Afficher la solution du problème";i['sleep']="Sommeil (si le mot de réveil est activé)";i['takeRookWithQueen']="Capturer la tour avec la dame";i['thisBlogPost']="Ce billet de blogue";i['turnOffVoiceRecognition']="Désactiver la reconnaissance vocale";i['voiceCommands']="Commandes vocales";i['watchTheVideoTutorial']="Visionner le tutoriel vidéo"})()