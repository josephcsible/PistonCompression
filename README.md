# PistonCompression

## Basics

### What does this mod do?
This mod allows pistons to compress certain blocks into other blocks or items.

### How do I use this mod?
You need Minecraft Forge installed first. Once that's done, just drop
pistoncompression-*version*.jar in your Minecraft instance's mods/
directory. Optionally, you can configure it to taste (see below).

### How do I compress blocks?
You need seven of the block you want to compress and six pistons. You also need
a means of powering the pistons, as well as blocks to stop pistons from being
able to move each other.
1. Place a block to be compressed, and place six more of it on its six faces.
2. Place pistons on each outer block that point inward to the center block.
3. Ensure that none of the pistons can accidentally move each other.
4. Activate all of the pistons.
5. Once the pistons extend, deactivate them and remove the compressed block.

### What settings does this mod have?
You can choose exactly what compressions are possible. The first two columns of
each entry are the old block's name and a state predicate, the same as
/testforblock uses. The second two columns are the new block's or item's name
and a data value or block state. The final column is optional. Examples:
- minecraft:ice * minecraft:packed_ice
- minecraft:coal_block * minecraft:diamond

The first entry will allow ice blocks to be compressed into a packed ice block.
The second entry will allow coal blocks to be compressed into a diamond.

## Development

### How do I compile this mod from source?
You need a JDK installed first. Start a command prompt or terminal in the
directory you downloaded the source to. If you're on Windows, type
`gradlew.bat build`. Otherwise, type `./gradlew build`. Once it's done, the mod
will be saved to build/libs/pistoncompression-*version*.jar.

### How can I contribute to this mod's development?
Send pull requests. Note that by doing so, you agree to release your
contributions under this mod's license.

## Licensing/Permissions

### What license is this released under?
It's released under the GPL v2 or later.

### Can I use this in my modpack?
Yes, even if you monetize it with adf.ly or something, and you don't need to
ask me for my permission first.
