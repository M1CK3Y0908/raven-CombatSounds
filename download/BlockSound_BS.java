//dev. M1CK3Y
int lastHurtTime = 0;
float lastHealth = -1.0f;

void onLoad() {
    modules.registerDescription(
        "v1.0.0 dev. M1CK3Y"
    );

    modules.registerSlider(
        "Sound",
        "",
        0,
        new String[] {
            "Anvil",
            "Orb",
            "Click",
            "Bow",
            "Pop",
            "Splash",
            "Fire",
            "Note",
            "Glass",
            "Blaze",
            "Ender"
        }
    );

    modules.registerSlider(
        "Volume",
        "",
        1.00,
        0.00,
        1.00,
        0.01
    );

    modules.registerSlider(
        "Pitch",
        "",
        1.80,
        0.50,
        2.00,
        0.10
    );
}

void onEnable() {
    lastHurtTime = 0;

    Entity player = client.getPlayer();

    if (player != null) {
        lastHealth = player.getHealth();
    } else {
        lastHealth = -1.0f;
    }
}

void onDisable() {
    lastHurtTime = 0;
    lastHealth = -1.0f;
}

void onPreUpdate() {
    Entity player = client.getPlayer();

    if (player == null) {
        return;
    }

    int hurtTime = player.getHurtTime();
    float health = player.getHealth();

    boolean newHurtEvent = false;

    if (hurtTime > lastHurtTime) {
        newHurtEvent = true;
    }

    if (lastHealth >= 0.0f && health < lastHealth) {
        newHurtEvent = true;
    }

    boolean blockhitting =
        player.isUsingItem() &&
        player.isHoldingWeapon();

    if (newHurtEvent && blockhitting) {

        int soundIndex = (int) modules.getSlider(
            scriptName,
            "Sound"
        );

        float volume = (float) modules.getSlider(
            scriptName,
            "Volume"
        );

        float pitch = (float) modules.getSlider(
            scriptName,
            "Pitch"
        );

        if (volume > 0.0f) {
            String sound = getSound(soundIndex);

            client.playSound(
                sound,
                volume,
                pitch
            );
        }
    }

    lastHurtTime = hurtTime;
    lastHealth = health;
}

String getSound(int index) {

    switch (index) {

        case 0:
            return "random.anvil_land";

        case 1:
            return "random.orb";

        case 2:
            return "random.click";

        case 3:
            return "random.bow";

        case 4:
            return "random.pop";

        case 5:
            return "random.splash";

        case 6:
            return "fire.ignite";

        case 7:
            return "note.pling";

        case 8:
            return "random.glass";

        case 9:
            return "mob.blaze.hit";

        case 10:
            return "mob.endermen.portal";

        default:
            return "random.anvil_land";
    }
}