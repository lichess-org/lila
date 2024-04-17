import contactEmail from './bits.contactEmail';

site.load.then(() => {
  location.hash ||= '#help-root';
  contactEmail();
});
