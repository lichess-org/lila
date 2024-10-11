"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.streamer)window.i18n.streamer={};let i=window.i18n.streamer;i['allStreamers']="Tous les streamers";i['approved']="Votre stream est approuvé.";i['becomeStreamer']="Devenir un streamer sur Lichess";i['changePicture']="Modifier/supprimer votre photo";i['currentlyStreaming']=s("En cours de streaming %s");i['downloadKit']="Téléchargez le kit streamer";i['doYouHaveStream']="Avez-vous une chaîne Twitch ou YouTube ?";i['editPage']="Modifier votre page de streamer";i['headline']="Titre";i['hereWeGo']="Allons-y !";i['keepItShort']=p({"one":"Restez brief %s caractère max","other":"Restez brief %s caractères max"});i['lastStream']=s("Dernière diffusion %s");i['lichessStreamer']="Lichess streamer";i['lichessStreamers']="Streamers sur Lichess";i['live']="EN DIRECT !";i['longDescription']="Description détaillée";i['maxSize']=s("Taille max : %s");i['offline']="HORS LIGNE";i['optionalOrEmpty']="Optionnel. Laisser vide si aucun";i['pendingReview']="Votre stream est en cours de révision par les modérateurs.";i['perk1']="Obtenez une flamboyante icône de streamer sur votre profil Lichess.";i['perk2']="Soyez remonté en haut de la liste des streamers.";i['perk3']="Notifier vos suiveurs sur Lichess.";i['perk4']="Montrez votre stream dans vos parties, tournois et études.";i['perks']="Avantages du streaming avec le mot-clé";i['pleaseFillIn']="Veuillez remplir vos informations de streamer et envoyer une photo.";i['requestReview']="demander une revue par un modérateur";i['rule1']="Incluez \\\"lichess.org\\\" dans votre titre de streaming lorsque vous streamez sur Lichess.";i['rule2']="Retirez le mot-clé lorsque vous ne streamez pas à propos de Lichess.";i['rule3']="Lichess détectera automatiquement votre stream et activera les avantages suivants :";i['rule4']=s("Lisez la %s pour assurer l\\'esprit sportif pour tous pendant la diffusion.");i['rules']="Règles de streaming";i['streamerLanguageSettings']="La page de diffusion en continu de Lichess cible votre public avec la langue fournie par votre plateforme de diffusion. Définissez la langue par défaut pour vos diffusions dans l\\'application ou le service que vous utilisez.";i['streamerName']="Votre nom de streamer sur Lichess";i['streamingFairplayFAQ']="FAQ sur l\\'esprit sportif pendant les diffusions";i['tellUsAboutTheStream']="Décrivez votre stream en une phrase";i['twitchUsername']="Votre nom d\\'utilisateur Twitch ou votre URL";i['uploadPicture']="Téléverser une image";i['visibility']="Visible sur la page des streamers";i['whenApproved']="Lorsque approuvé par les modérateurs";i['whenReady']=s("Lorsque vous êtes prêt à être listé en tant que streamer Lichess, %s");i['xIsStreaming']=s("%s est en train de streamer");i['xStreamerPicture']=s("%s photo de streamer");i['yourPage']="Votre page de streamer";i['youTubeChannelId']="ID de votre chaîne YouTube"})()