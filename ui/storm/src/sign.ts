export default function(serverKey: string): Promise<string> {
  const otp = randomAscii(64);
  lichess.socket.send('sk1', `${serverKey}:${otp}`);
  return new Promise(solve =>
    lichess.pubsub.on('socket.in.sk1', encrypted => solve(xor(encrypted, otp)))
  );
}

function xor(a: string, b: string) {
  const result = [];
  for (let i = 0; i < a.length; i++)
    result.push(String.fromCharCode(a.charCodeAt(i) ^ b.charCodeAt(i)));
  return result.join('');
}

function randomAscii(length: number) {
  let result = '';
  const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  const charactersLength = characters.length;
  for (let i = 0; i < length; i++)
    result += characters.charAt(Math.floor(Math.random() * charactersLength));

  return result;
}
