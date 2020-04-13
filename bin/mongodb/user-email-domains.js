const domains = {};
db.user4.find({email:{$exists:1}},{email:1}).forEach(u => {
  const domain = u.email.split('@')[1];
  domains[domain] = (domains[domain] || 0) + 1;
});
db.email_domains.drop();
Object.keys(domains).forEach(d => {
  db.email_domains.insert({_id:d,nb:domains[d]});
});
db.email_domains.createIndex({nb:-1}, {background:1});
