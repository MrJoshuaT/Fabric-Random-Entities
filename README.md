# Fabric Random Entities

The goal of this mod is to replicate the Optifine random entities feature set, but in Fabric.

This project was started as a personal project, so I cannot gaurantee and stability, performance or compatibility, until more testing and improvements are done. I just wanted to play with Optifines random entities but also wanted to use performance mods such as Sodium as well.

## What can it do?

* Random textures based upon .properties rules and generic variations
* Eyes textures for picked random texture (should support phantoms, enderman, spiders)
* Villager outer layer textures
* Should support modded gameplay, the code should not make and distictions between vanilla and not

## What can it not do? (for now)
* Properties name rule is not adhered to at all, and is ignored for time being
* Other feature renderers, for like Withers etc
* Support for paintings
* Not tested all entities, such as horses, wolfs etc

## Other things
* Code cleanup and refactor, specially improve the `EyesFeatureRendererMixin.render` and remove `// Legit ewwwwww noises` comment
* Add proper caching, only some are in for now to help with some cases I experienced
* Clean up logs, because I currently dump a mertric amount, because of testing
