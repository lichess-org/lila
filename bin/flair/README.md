# Lichess flair collection

Lichess reads from this repository to get the list of available flairs.
If you want to add a flair, please submit a pull request.

The available flairs are visible at https://lichess1.org/assets/flair/index.html.

Lichess flairs are not animated.

## Adding a flair

### Constraints

Flair images must:

- be webp, more on this below
- not be animated
- be square
- have a width between 60px and 100px (most are 60px)
- have a transparent background
- have a known license allowing use within Lichess
- be nice, respectful, not controversial and not offensive

### Where to get flairs

This is probably a good source for new flairs: https://slackmojis.com/.

Feel free to make your own, or to get them from somewhere else - but be mindful of licenses.

### Webp

You can convert your png to webp with `cwebp`:

```shell
cwebp path/to/horsey.png -o lila/public/flair/img/activity.lichess-horsey.webp
```

Similar programs are available, such as [ImageMagick](https://imagemagick.org/index.php).

### Naming

The name of the file is important, it's the only metadata we have. It's made of two parts: the category, and the name.

`<category>.<multi-word-name>.webp`

The category will be used to group the flairs in the UI. It can be one of the following:
`smileys`, `people`, `nature`, `food-drink`, `activity`, `travel-places`, `objects`, `symbols`.

The name will be used to identify the flair in the UI. It can be any string, but it should be short and descriptive.
It can only contain letters, numbers, and dashes. Descriptive names help users find the flair.

### Compile the flair list

Optional. You can add and remove flair images without compiling the flair list,
and wait for someone else to compile it before it comes online.

[Save](https://github.com/lichess-org/lila/pull/18368#issuecomment-3385711626) https://emojipedia.org/google into bin/flair/emojipedia.html.

Run the generation script:

```shell
pnpx tsx bin/flair/generate.ts
```

It will update the `public/flair/list.txt` file which you must commit along with the updated flair images.

Lichess reads `public/flair/list.txt` periodically to update its flair database.
