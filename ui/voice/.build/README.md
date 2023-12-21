# overview:

```
the idea is that no transform on a heard phrase should be allowed if the sum of its
substitution costs exceed some value (currently 1.0). the makeGrammar script generates json
arrays defining word grammars (tokens, substitution rules, etc) at grammar/<type-lang>.json.
these are built from the inputs in ./lexicon/*.json and hopefully a crowdv.json

crowdv.jsons are kept in the lifat repo due to their size, they'll be downloaded as
needed by makeGrammar

the lexicon folder contains <grammar>-lex.jsons and optional <grammar>-patch.jsons (where
manual substitutions can be defined). patch entries should only be applied to
vocabulary words that lack a sufficient sample in the crowdv data.

```

# usage:

```
node dist/makeGrammar.js --freq=.002 \ # default values given explicitly here
                         --count=6 \
                         moves-en # give the lexicon/patch prefix as a standard argument
```

## arguments:

```
  {freq} is the minimum frequency of a valid substitution. frequency is calculated as the
    number of input phrases corrected by the substitution divided by the total number of
    occurrences of the token the substitution operates on.

  {count} is minimum occurrences of a substitution in the input data

  {in} is a crowdv json resolved in the following manner:
    - as a local file either absolute or relative to the .build/crowdv directory
    - as a https url
    - otherwise, downloaded from https://raw.githubusercontent.com/lichess-org/lifat/dev/crowdv

  {grammar}
```

# some terminology:

```

  grammar - a list of entries for input words recognized by kaldi

  entry - a single entry in the grammar contains the input word (sometimes more than one), a
    token representation, a value code, classification tags, and a substitution list

  word - a unit of recognition corresponding to the "in" field of a single grammar entry.
    a word can also be a phrase that is treated as a single unit. for example - "long castle",
    "queen side castle".

  phrase - one or more words separated by spaces

  tok - short for token, a single char uniquely representing the kaldi-recognizable characteristics
    of an input word.

  val - the value of a token/word in terms that move & command processing logic understands.
    mappings from token/words to vals are not bijective - the input words 'captures' and 'takes'
    have the same value but not the same token.

  toks - a phrase tokenized as a string with no spaces or separators

  vals - a phrase as vals separated by commas

  htoks - a heard phrase as token string, may contain errors or ambiguities

  hvals - a heard phrase in the val space (i.e. comma separated vals)

  xtoks - an exact phrase in the token space

  xvals - an exact phrase in the val space

  in voiceMove, class lookup methods for lexicon entry fields take the form <fromTo> where <from>
  is the input field and <to> is the output. for example: tokWord fetches the input word corresponding
  to a token, and wordTok fetches the token corresponding to an input word

  for any variable prefixed with x or h, x means exact and h means heard. another way to think of it
  is:  x is what we try to map h to when performing substitutions.
```

# see also:

```
  * src/makeGrammar.ts
  * ui/site/component/mic.ts
  * ui/voice/src/plugins/voiceMove.ts
```
