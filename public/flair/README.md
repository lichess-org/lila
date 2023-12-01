# Lichess flair collection

Lichess reads from this repository to get the list of available flairs.
If you want to add a flair, please submit a pull request.

The available flairs are visible at https://lichess1.org/assets/flair/index.html.

Lichess flairs are not animated.

## Adding a flair

### Where to get flairs

This is probably a good source for new flairs: https://slackmojis.com/.

Feel free to make your own, or to get them from somewhere else - but be mindful of licenses.

### Flair format

Lichess only uses webp flairs. You can convert your png to webp with `cwebp`:

```shell
cwebp path/to/horsey.png -o lila/public/flair/img/activity.lichess-horsey.webp
```

Similar programs are available, such as [ImageMagick](https://imagemagick.org/index.php).

Now that you have a webp file, you can add it to the `flair/img` directory.
The name of the file is important, it's the only metadata we have. It's made of two parts: the category, and the name.

Flairs must have a transparent background.

`<category>.<multi-word-name>.webp`

The category will be used to group the flairs in the UI. It can be one of the following:
`smileys`, `people`, `nature`, `food-drink`, `activity`, `travel-places`, `objects`, `symbols`, `flags`.

The name will be used to identify the flair in the UI. It can be any string, but it should be short and descriptive.
It can only contain letters, numbers, and dashes. Descriptive names help users find the flair.

### Compile the flair list

Optional. You can add and remove flair images without compiling the flair list,
and wait for someone else to compile it before it comes online.

If you have a command line and want to compile the flair list, run the following:

```shell
./flair/list.sh
```

It will update the `flair/list.txt` file which you must commit along with the updated flair images.

Lichess reads `flair/list.txt` periodically to update its flair database.
