// import { MongoClient } from 'mongodb';
//
// const oauthToken = process.env.OAUTH_TOKEN;
// const client = new MongoClient('mongodb://127.0.0.1:27917/lichess');
// const lichessUrl = 'http://l.org';
// const dryRun = true;
//
// // hardcode translations here
// const translations = {
//   en: `Upgrade to our new app: https://lichess.org/app`,
// };
//
// const count = client.db().collection('lm_user_recent_nomobile').estimatedDocumentCount();
//
// console.log(`Sending messages to ${count} users...`);
//
// const chunkSize = 100;
// const chunk = [];

// client.db().collection('lm_user_recent_nomobile').find().forEach(user => {
//   // group by chunks of 100 users
//   chunk.push(user._id);
//   if (chunk.length >= chunkSize) processChunk();
// });
//
// async function processChunk() {
//
//   const users = db.user4.find({ _id: { $in: chunk } },{$project:{lang:1}}).toArray();
//
//   if (dryRun) console.log(users.map(u => `${u._id} (${u.lang})`).join('\n'));
//   else {
//     const res = await fetch(`${lichessUrl}/inbox/${username}`, {
//       method: 'POST',
//       headers: {
//         'Content-Type': 'application/x-www-form-urlencoded',
//         Authorization: `Bearer ${oauthToken}`,
//       },
//       body: new URLSearchParams({ text }),
//     });
//
//     if (!res.ok) console.error(`Failed to send message to ${username}: ${res.status} ${res.statusText}`);
//   }
//
//   await new Promise(resolve => setTimeout(resolve, 500)); // Avoid hitting rate limits
// });
//
// function makeMessage(langCode, url) {
//   const lang = langCode.slice(0, 2);
//   const translated = translations[lang] || translations.en;
//   return translated.replace('{URL}', url);
// }
