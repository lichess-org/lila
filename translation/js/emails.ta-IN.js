"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("எங்களை தொடர்பு கொள்ள, %s ஐப் பயன்படுத்தவும்.");i['common_note']=s("இது நீங்கள் %sஐப் பயன்படுத்துவது தொடர்பான சேவை மின்னஞ்சல்.");i['common_orPaste']="(சொடுக்குவது செயல்படவில்லையா? அதை உங்கள் உலாவியில் ஒட்ட முயலவும்!)";i['emailChange_click']="இந்த மின்னஞ்சலுக்கான அணுகல் உங்களிடம் இருப்பதை உறுதிப்படுத்த, கீழே உள்ள இணைப்பைச் சொடுக்கவும்.";i['emailChange_intro']="உங்கள் மின்னஞ்சல் முகவரியை மாற்றுமாறு கோரியுள்ளீர்கள்.";i['emailChange_subject']=s("புதிய மின்னஞ்சல் முகவரியை உறுதிசெய்க, %s");i['emailConfirm_click']="உங்கள் Lichess கணக்கைச் செயல்படுத்த இந்த இணைப்பைச் சொடுக்கவும்:";i['emailConfirm_ignore']="நீங்கள் Lichess உடன் பதிவு செய்யவில்லை என்றால், இந்தச் செய்தியை நீங்கள் பாதுகாப்பாகப் புறக்கணிக்கலாம்.";i['emailConfirm_subject']=s("உங்கள் lichess.org கணக்கை உறுதிசெய்க, %s");i['logInToLichess']=s("lichess.org க்குள் உள் நுளை, %s");i['passwordReset_clickOrIgnore']="இந்த கோரிக்கையை நீங்கள் செய்திருந்தால், கீழேயுள்ள இணைப்பை சொடுக்கவும். இல்லையெனில், இந்த மின்னஞ்சலை நீங்கள் புறக்கணிக்கலாம்.";i['passwordReset_intro']="உங்கள் கணக்கிற்கான கடவுச்சொல்லை மீட்டமைப்பதற்கான கோரிக்கையைப் பெற்றுள்ளோம்.";i['passwordReset_subject']=s("உங்கள் lichess.org கடவுச்சொல்லை மீட்டமைக்க, %s");i['welcome_subject']=s("lichess.org க்கு வரவேற்கிறோம், %s");i['welcome_text']=s("Https://lichess.org இல் உங்கள் கணக்கை வெற்றிகரமாக உருவாக்கியுள்ளீர்கள்.\nஇதோ உங்கள் சுயவிவரப் பக்கம்: %1$s. நீங்கள் அதை %2$s இல் தனிப்பயனாக்கலாம்.\nவிளையாடி மகிழுங்கள், உங்கள் காய்கள் எப்பொழுதும் எதிரியின் ராஜாவை நோக்கிச் செல்லட்டும்!")})()