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
- **Dev panel** (development only) in the top left while riding.

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
      ai/                             goals: foal following, skittish fleeing, curious wandering
      mixin/AbstractHorseMixin.java   server hooks: feeding, riding time, hunger, mount status, goals,
                                      hoofbeats per gait
      mixin/AnimalBreedingMixin.java  sets up a bred foal (family, taming, personality)
      mixin/RandomStrollGoalMixin.java  foals stroll less and stay near family
      mixin/PanicGoalMixin.java       brave horses don't panic when hit
      mixin/MobAccessor.java          access to a mob's goals
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
    src/client/resources/
      assets/horseloyalty/lang/en_us.json                 all text shown to players
      assets/horseloyalty/textures/gui/sprites/hud/       the horseshoe icons

Why riding is client code: Minecraft moves a player-ridden horse on that player's own
client and sends the result to the server, so the gait and steering hooks only ever run
there. Loyalty, feeding and hunger are decided by the server.

## Dev panel

Shows loyalty, points, hunger, gait and speed while riding. It is on in `runClient` and off
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
