import cryptoRandomString from 'crypto-random-string';

export function initModule(id: string = 'form3-newPasswd1'): void {
  const password = cryptoRandomString({ length: 20, type: 'ascii-printable' });
  $('#' + id)
    .val(password)
    .trigger('input');
}
