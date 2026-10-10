# Android ECDICT integration

Source: https://github.com/skywind3000/ECDICT (MIT).
Snapshot: `bc015ed2e24a7abef49fc6dbbb7fe32c1dadaf8b`.
Original license: `app/src/main/assets/ecdict/LICENSE.txt`.

761,816 English word/phrase entries are bundled in 256 JSON shards from the full
upstream CSV. Lookup reads only one shard on the IO dispatcher and works offline.
Automatic lookup prefers ECDICT for English; the settings page also offers explicit
ECDICT selection. Chinese reverse lookup is not supported by this English-to-Chinese
dataset; use Google instead. Original definitions (mostly simplified Chinese) are
preserved. No examples are fabricated; existing AI completion remains optional.

The manifest stores the upstream revision and CSV SHA-256 for provenance.
The PWA sibling repository includes `scripts/build-ecdict.py`, the reproducible
converter. Run it with this repository's `app/src/main/assets` directory as its
optional argument to regenerate these assets from the pinned public source.
