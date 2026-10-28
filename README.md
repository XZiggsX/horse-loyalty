# Horse Loyalty

A Fabric mod for Minecraft Java 26.3 that makes horses bond with their riders.
Horses, donkeys, mules and skeleton/zombie horses are affected; llamas and camels are not.

## Features

- **Loyalty levels 0 to 5**, tracked separately for every rider: 0 Untamed, 1 Unruly,
  2 Wary, 3 Trusting, 4 Devoted, 5 Partners. Earned by feeding (with a cooldown) and by
  riding a moving horse. Shown in the horse inventory and when you mount.
- **Gaits**: tap W to shift up (Walk, Trot, Canter, Gallop), tap S to shift down.
  The horse keeps its gait without holding a key. Each gait is a share of the horse's top
  speed (30/50/75/100% by default). Press S again at a halt and hold it to back up.
- **Pushing at a gallop**: keep tapping W at a gallop to push the horse 2% faster per tap, up to
  120% of its gallop. Stop tapping for 1.5 seconds and the extra speed fades away, halving every
  second, back to a normal gallop. While pushed the horse breathes hard (quietly, only for you)
  and the HUD gait bar glows.
- **Balking**: a horse that barely knows you (loyalty 1 to 3) may refuse to go from a walk to a trot.
  It prances instead, and won't go faster for 10 seconds; a skittish one may throw you off. Which
  shifts can fail, and how likely, is in the config.
- **Sidestepping**: hold sprint (Ctrl) with A or D and the horse moves at an angle (45° by
  default) without turning; from a halt it steps straight sideways.
- **Boat-style steering**: A and D turn at a fixed rate, so faster gaits make wider turns.
- **Loyalty matters**: it scales the speed of every gait, sets the turn rate, and sets how often the
  horse drops a gait on its own (level 5: a fixed 10 minute timer without input, against AFK riding).
- **Hunger**: a tamed horse that isn't fed or left to graze on grass, mycelium or podzol
  shows dark particles and slowly loses loyalty with every rider, down to level 1.
- **Foals**: a foal you breed is born tamed to you, with 25% of your average loyalty with
  its parents. It picks one parent at random to follow (roleplay it as mother or father),
  follows you when that parent isn't near, and wanders less. Wild foals act as in vanilla.
- **Personalities**: every horse is Steady, Brave (doesn't panic when hit), Skittish (flees
  creepers, and bolts from them while ridden unless loyalty is high), Curious (wanders toward
  villages when loose) or Lazy (drops gaits more often). Shown in the horse inventory from
  loyalty 2. Foals take after their parents, so you can breed for a personality.
- **Calling with a goat horn**: sneak and use a goat horn on a tamed horse you have loyalty 2
  (Wary) with to bind it. The horn shines, takes the horse's name if it has one, and says
  "Calls <name>". Blowing it calls the horse if it is loaded: within 24 blocks it runs to you,
  further away it is moved to open ground behind you, out of sight, and runs to you from there.
  Not in the Nether or underground (below sea level or deep under the surface). A horse that is
  ridden, leashed or in a boat can't come, and one in another dimension only comes at loyalty 5.
- **Visual and sound feedback**: while riding, five horseshoes above the horse's hearts show your
  loyalty and a four-step bar shows the gait. Hoofbeats change with the gait, cantering and
  galloping horses kick up dust (splashes in water), and in snowy places they breathe out little
  puffs. Everyone nearby sees and hears this, and hears a horse snort when it drops a gait or bolts.
  A loyalty level up gets a whinny and a toast; a level lost to hunger gets a low snort and a toast.
  A horse that balks rears up with a snort for everyone, and you get a toast saying what happened.
  Each part can be turned off under `feedback` in the config.
- **Config** in `config/horseloyalty.json` for all of the numbers above. On a server, the server's
  riding numbers (speeds, turning, gait drops, balking, pushing) apply to everyone, whatever their
  own file says; HUD and sound settings stay each player's own.
- **Genetics** (see below): hidden genes decide a horse's coat, markings and personality, so
  foals can show colours their parents only carry.
- **Dev panel** (development only) in the top left while riding, and `/horsegenes` for operators.

## Genetics

Every horse carries two copies of 8 genes, one from each parent. A foal gets one random copy
of each gene from each parent, and each copy has a 1% chance (`genetics.mutationPercent`) to turn
into a random version. Genes never change stats: health, speed and jump still average the parents.

| Gene | Versions (strongest first) | Effect |
|---|---|---|
| red_black | black, red | two red: chestnut base |
| agouti | bay, seal_brown, black | on black pigment: brown, dark brown or black |
| cream | cream, none | one copy lightens to creamy (except black); two make a chestnut white (cremello) and any other base creamy |
| gray | gray, none | one copy turns any coat gray |
| pinto | pinto, none | one copy: white patches |
| leopard | leopard, none | one copy: white dots, two: black dots |
| socks | none, socks | two copies: white legs |
| temperament | brave, skittish, curious, lazy, steady | any quirk beats steady; two different quirks are 50/50, picked at birth |

Wild herds descend from two founders that fit the herd's vanilla coat, so herds look related.
Horses from before genetics (or with it just turned on) get genes that fit how they already look.
Donkeys and mules only use the temperament gene. Turn it all off with `genetics.enabled`; then
coats are vanilla again and personalities use the old inherit odds.

### Horse info screen

The horse inventory has two small tabs on its top-right edge: **Inventory** and **Info**. Info
swaps the slots for an overview in a larger window: the horse, its kind and personality, your
loyalty as horseshoes, health, speed and jump bars, its parents' names, its real coat and
marking names (Buckskin, Palomino, Cremello, Smoky black, Tobiano, Fewspot leopard...), and a
table of its genes with both copies written out in full (Extension, Agouti, Cream, Grey, Tobiano,
Leopard complex, White socks, Temperament). Coloured copies decide the look; grey ones are only
carried and can show in foals. The table scrolls when the window is too small. Donkeys and mules
only show temperament. The screen always opens on the inventory, the Info tab can't be used with
an item on the cursor, and with genetics off (on the server) there are no tabs at all.
"White socks" is a made-up gene for the mod; the others are named after real horse genes.

Operators (permission level 2) can test it with `/horsegenes` on the horse they look at:
`show`, `set <gene> <v1> <v2>`, `mark` (horse A), `predict` (exact foal odds for the marked
horse and this one) and `simulate <count>` (breeds that many foals on paper).

## Requirements

- JDK 25 (Minecraft 26.x needs it). Check with `java -version`.
- IntelliJ IDEA 2025.3 or newer is recommended for Java 25.

## Run

    ./gradlew runClient     # launch Minecraft with the mod (dev username "XZiggsX")
    ./gradlew build         # jar ends up in build/libs/

## Layout

Fabric splits the code in two source sets. `main` is loaded on both the server and the
client; `client` is only loaded on the client and never on a dedicated server.

    src/main/java/.../
      HorseLoyaltyMod.java            common entrypoint: loads config, registers saved data
      config/HorseLoyaltyConfig.java  config/horseloyalty.json, defaults and validation
      config/RidingRules.java         the server's riding numbers, sent to every player who joins
      loyalty/Loyalty.java            loyalty levels, per-rider points, level up/down (server)
      loyalty/LoyaltyData.java        what a horse remembers about one rider (saved)
      loyalty/Hunger.java             hunger timer, particles and loyalty loss (server)
      loyalty/Family.java             a bred foal's parents and breeder, taming at birth (saved)
      loyalty/LoyaltyChangePayload.java  tells a player their level changed (for the toast)
      personality/Personality.java    personalities, rolling and inheritance (saved, synced)
      riding/GaitSync.java            the rider's gait, sent to the server and synced to everyone
      horn/HornBinding.java           binding a goat horn to a horse (sneak + use)
      horn/HornCall.java              blowing a bound horn: finds the horse, moves it if needed
      ai/                             goals: foal following, skittish fleeing, curious wandering,
                                      answering a horn call
      mixin/AbstractHorseMixin.java   server hooks: feeding, riding time, hunger, mount status, goals,
                                      hoofbeats per gait
      genetics/core/                  genes, inheritance, odds and genomes that fit a look (no Minecraft code)
      genetics/trait/                 how genes become a coat, markings and a personality
      genetics/world/                 genes saved on horses, herd founders, HorseGenetics (the entry point)
      genetics/view/GeneticsView.java read-only genetics, for the dev tools and a future screen
      dev/HorseGenesCommand.java      /horsegenes, operator-only genetics testing
      horseinfo/                      read-only HorseProfile for the info screen, reveal policy
      util/HorseUnits.java            speed and jump in blocks, vanilla stat ranges (riding, dev panel, info screen)
      mixin/AnimalBreedingMixin.java  sets up a bred foal (family, taming, genes or personality)
      mixin/HorseSpawnMixin.java      gives wild and summoned horses genes from their herd's founders
      mixin/HorseGroupDataMixin.java  lets a spawn group hold its herd's founders
      mixin/HorseInvoker.java         access to a horse's coat and markings setter
      mixin/RandomStrollGoalMixin.java  foals stroll less and stay near family
      mixin/PanicGoalMixin.java       brave horses don't panic when hit
      mixin/MobAccessor.java          access to a mob's goals
      mixin/InstrumentItemMixin.java  blowing a bound goat horn calls its horse
    src/main/resources/
      data/horseloyalty/tags/block/grazing_blocks.json   blocks a horse can graze on

    src/client/java/.../
      client/HorseLoyaltyClient.java  client entrypoint
      client/riding/Gait.java         gaits and their speeds
      client/riding/Riding.java       speed caps, turn rate and gait drops per loyalty level
      client/riding/GaitHolder.java   lets client code read a horse's gait
      client/riding/ClientGait.java   a horse's gait on this client: your own, or the synced one
      client/riding/GaitRaise.java    the chance a horse refuses to go faster (balking), and its cooldown
      client/riding/GallopPush.java   pushing a gallop with W taps: extra speed, fade-out, breathing
      client/feedback/RidingHud.java  loyalty horseshoes and gait bar while riding
      client/feedback/GaitEffects.java    dust, splashes and cold breath
      client/feedback/LoyaltyToasts.java  toast when your loyalty level changes
      client/feedback/RidingToasts.java   toasts about the ride, such as a horse balking
      client/dev/LoyaltyDebugHud.java dev panel (off in release builds)
      mixin/client/AbstractHorseRidingMixin.java          gaits, steering, gait drops, skittish bolts
      mixin/client/AbstractMountInventoryScreenMixin.java loyalty label in the horse inventory
      mixin/client/HudMixin.java                          draws the dev panel
      client/horseinfo/                                   the horse info screen: tabs, layout, sections
      mixin/client/MountInfoScreenMixin.java              adds the info screen to the horse inventory
      mixin/client/ContainerScreenInfoMixin.java          blocks the slots while the info screen shows
    src/client/resources/
      assets/horseloyalty/lang/en_us.json                 all text shown to players
      assets/horseloyalty/textures/gui/sprites/hud/       the horseshoe icons

Why riding is client code: Minecraft moves a player-ridden horse on that player's own
client and sends the result to the server, so the gait and steering hooks only ever run
there. Loyalty, feeding and hunger are decided by the server.

## Dev panel

Shows loyalty, points, hunger, gait and speed while riding, plus the horse's genes, what it
shows and whether its parents are known. Looking at a horse shows just the genetics part. It is on in `runClient` and off
in a built jar automatically. Force it with `-Dhorseloyalty.devhud=true` or `=false`.
To remove it completely, delete `client/dev/`, `mixin/client/HudMixin.java` and the
`"HudMixin"` line in `horseloyalty.client.mixins.json`.

## Known limits

- Speed, turning and gait drops use each player's own config, since riding runs on their
  client. Before a public release the server should send its config to joining players.
- Others hear a gait drop or a bolt and see the gait (dust, hoofbeats), but a bolt has no
  animation of its own yet: to them it looks like a gallop.
- `feedback.gaitSounds` is read by the server, since it plays the hoofbeats for everyone. The
  other `feedback` settings are each player's own.

## License

MIT, see `LICENSE`.
