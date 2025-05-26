export type KeyboardMapping = 'NONE' | 'QWERTY';

interface Mapping {
  key: string;
  code: string;
  shiftKey: boolean;
}

export class KeyboardRemapper {
  mapping: KeyboardMapping = 'NONE';

  constructor(mapping?: KeyboardMapping) {
    if (mapping !== undefined) this.mapping = mapping;
  }

  map(e: KeyboardEvent): { mapped: boolean; key: string } {
    let mappingFound: Mapping | undefined = undefined;
    if (this.mapping === 'QWERTY') {
      mappingFound = qwertyMappings.find(item => item.code === e.code && item.shiftKey === e.shiftKey);
    }
    if (mappingFound != undefined) return { mapped: true, key: mappingFound.key };
    else return { mapped: false, key: e.key };
  }
}

const qwertyMappings: Mapping[] = [
  ...qwertyMapUpperCaseLetters(),
  ...qwertyMapLowerCaseLetters(),
  ...qwertyMapDigits(),
  {
    key: '?',
    code: 'Slash',
    shiftKey: true,
  },
  {
    key: '=',
    code: 'Equal',
    shiftKey: false,
  },
  {
    key: '-',
    code: 'Minus',
    shiftKey: false,
  },
  {
    key: '+',
    code: 'Equal',
    shiftKey: true,
  },
  {
    key: '#',
    code: 'Digit3 ',
    shiftKey: true,
  },
];

const alphabet: string[] = Array.from(
  { length: 26 },
  (_, i) => String.fromCharCode(97 + i), // 'a' is char code 97
);

function qwertyMapLowerCaseLetters(): Mapping[] {
  return alphabet.map(
    (letter: string) => <Mapping>{ key: letter, code: 'Key' + letter.toUpperCase(), shiftKey: false },
  );
}

function qwertyMapUpperCaseLetters(): Mapping[] {
  return alphabet.map(
    (letter: string) =>
      <Mapping>{ key: letter.toUpperCase(), code: 'Key' + letter.toUpperCase(), shiftKey: true },
  );
}

const digits: string[] = Array.from(
  { length: 10 },
  (_, i) => String.fromCharCode(48 + i), // '0' is char code 48
);

function qwertyMapDigits(): Mapping[] {
  return digits.map(
    (letter: string) => <Mapping>{ key: letter, code: 'Digit' + letter.toUpperCase(), shiftKey: false },
  );
}
