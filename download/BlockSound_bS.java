// dev. M1CK3Y
int lastHurtTime = 0;
float lastHealth = -1.0f;

int MAX_PENDING = 10;

Entity[] pendingEntity = new Entity[MAX_PENDING];
float[] pendingHealth = new float[MAX_PENDING];
long[] pendingTime = new long[MAX_PENDING];
boolean[] pendingCrit = new boolean[MAX_PENDING];

String[] soundOptions = new String[] {
    "None",
    "Neverlose",
    "Fatality",
    "gamesense",
    "Hitmarker",
    "Primordial",
    "Rust Headshot",
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
};

void onLoad() {

    modules.registerDescription(
        "v1.2.0 | dev. M1CK3Y"
    );

    modules.registerButton(
        "Attack",
        true
    );

    modules.registerButton(
        "Disable when critical",
        true
    );

    modules.registerSlider(
        "Attack Sound",
        "",
        0,
        soundOptions
    );

    modules.registerSlider(
        "Attack Volume",
        "%",
        100,
        0,
        100,
        1
    );

    modules.registerSlider(
        "Attack Pitch",
        "",
        1.80,
        0.50,
        2.00,
        0.10
    );

    modules.registerButton(
        "Critical",
        true
    );

    modules.registerSlider(
        "Critical Sound",
        "",
        0,
        soundOptions
    );

    modules.registerSlider(
        "Critical Volume",
        "%",
        100,
        0,
        100,
        1
    );

    modules.registerSlider(
        "Critical Pitch",
        "",
        1.80,
        0.50,
        2.00,
        0.10
    );

    modules.registerButton(
        "Hurt",
        true
    );

    modules.registerSlider(
        "Hurt Sound",
        "",
        0,
        soundOptions
    );

    modules.registerSlider(
        "Hurt Volume",
        "%",
        100,
        0,
        100,
        1
    );

    modules.registerSlider(
        "Hurt Pitch",
        "",
        1.80,
        0.50,
        2.00,
        0.10
    );

    modules.registerButton(
        "Block",
        true
    );

    modules.registerSlider(
        "Block Sound",
        "",
        0,
        soundOptions
    );

    modules.registerSlider(
        "Block Volume",
        "%",
        100,
        0,
        100,
        1
    );

    modules.registerSlider(
        "Block Pitch",
        "",
        1.80,
        0.50,
        2.00,
        0.10
    );
}

void onEnable() {

    lastHurtTime = 0;
    lastHealth = -1.0f;

    clearAllPending();

    Entity player = client.getPlayer();

    if (player != null) {
        lastHealth = player.getHealth();
    }
}

void onDisable() {

    lastHurtTime = 0;
    lastHealth = -1.0f;

    clearAllPending();
}

void onPreUpdate() {

    Entity player = client.getPlayer();

    if (player == null) {
        return;
    }


    checkPendingHits();

    int hurtTime = player.getHurtTime();
    float health = player.getHealth();

    boolean newHurtEvent = false;


    if (hurtTime > lastHurtTime) {
        newHurtEvent = true;
    }


    if (lastHealth >= 0.0f &&
        health < lastHealth) {

        newHurtEvent = true;
    }

    boolean blockhitting =
        player.isUsingItem() &&
        player.isHoldingWeapon();


    if (newHurtEvent) {

        if (blockhitting) {
            playBlockSound();
        } else {
            playHurtSound();
        }
    }


    lastHurtTime = hurtTime;
    lastHealth = health;
}

boolean onPacketSent(CPacket packet) {

    if (!(packet instanceof C02)) {
        return true;
    }


    C02 attack = (C02) packet;


    if (!"ATTACK".equals(attack.action)) {
        return true;
    }


    Entity target = attack.entity;


    if (target == null) {
        return true;
    }


    Entity player = client.getPlayer();


    if (player == null) {
        return true;
    }


    float health = target.getHealth();

    boolean crit = isCritical(player);


    addPendingHit(
        target,
        health,
        crit
    );


    return true;
}

boolean isCritical(Entity player) {

    if (player.getFallDistance() <= 0.0f) {
        return false;
    }

    if (player.onGround()) {
        return false;
    }

    if (player.isOnLadder()) {
        return false;
    }

    if (player.isInWater()) {
        return false;
    }

    return true;
}

void addPendingHit(
    Entity target,
    float health,
    boolean crit
) {

    long now = client.time();

    for (int i = 0; i < MAX_PENDING; i++) {

        if (pendingEntity[i] != null &&
            pendingEntity[i] == target) {

            pendingHealth[i] = health;
            pendingTime[i] = now;
            pendingCrit[i] = crit;

            return;
        }
    }

    for (int i = 0; i < MAX_PENDING; i++) {

        if (pendingEntity[i] == null) {

            pendingEntity[i] = target;
            pendingHealth[i] = health;
            pendingTime[i] = now;
            pendingCrit[i] = crit;

            return;
        }
    }

    int oldest = 0;


    for (int i = 1; i < MAX_PENDING; i++) {

        if (pendingTime[i] <
            pendingTime[oldest]) {

            oldest = i;
        }
    }


    pendingEntity[oldest] = target;
    pendingHealth[oldest] = health;
    pendingTime[oldest] = now;
    pendingCrit[oldest] = crit;
}

void checkPendingHits() {

    long now = client.time();


    for (int i = 0; i < MAX_PENDING; i++) {

        Entity target = pendingEntity[i];


        if (target == null) {
            continue;
        }

        if (now - pendingTime[i] > 500) {

            clearPending(i);

            continue;
        }


        float currentHealth;


        try {

            currentHealth = target.getHealth();

        } catch (Exception e) {

            clearPending(i);

            continue;
        }


        float damage =
            pendingHealth[i] -
            currentHealth;

        if (damage > 0.0f &&
            damage < 20.0f) {

            boolean crit =
                pendingCrit[i];

            if (crit) {

                boolean disableAttack =
                    modules.getButton(
                        scriptName,
                        "Disable when critical"
                    );

                if (!disableAttack) {
                    playAttackSound();
                }


                playCriticalSound();


            } else {

                playAttackSound();
            }


            clearPendingEntity(target);
        }

        else if (damage < 0.0f) {

            pendingHealth[i] =
                currentHealth;

            pendingTime[i] =
                now;
        }
    }
}

void clearPending(int index) {

    pendingEntity[index] = null;
    pendingHealth[index] = 0.0f;
    pendingTime[index] = 0;
    pendingCrit[index] = false;
}


void clearPendingEntity(Entity target) {

    for (int i = 0; i < MAX_PENDING; i++) {

        if (pendingEntity[i] == target) {
            clearPending(i);
        }
    }
}


void clearAllPending() {

    for (int i = 0; i < MAX_PENDING; i++) {
        clearPending(i);
    }
}

void playAttackSound() {

    if (!modules.getButton(
        scriptName,
        "Attack"
    )) {
        return;
    }


    int soundIndex =
        (int) modules.getSlider(
            scriptName,
            "Attack Sound"
        );


    // None
    if (soundIndex == 0) {
        return;
    }


    int volumePercent =
        (int) modules.getSlider(
            scriptName,
            "Attack Volume"
        );


    if (volumePercent <= 0) {
        return;
    }


    float volume =
        volumePercent / 100.0f;


    float pitch =
        (float) modules.getSlider(
            scriptName,
            "Attack Pitch"
        );


    client.playSound(
        getSound(soundIndex),
        volume,
        pitch
    );
}

void playCriticalSound() {

    if (!modules.getButton(
        scriptName,
        "Critical"
    )) {
        return;
    }


    int soundIndex =
        (int) modules.getSlider(
            scriptName,
            "Critical Sound"
        );


    // None
    if (soundIndex == 0) {
        return;
    }


    int volumePercent =
        (int) modules.getSlider(
            scriptName,
            "Critical Volume"
        );


    if (volumePercent <= 0) {
        return;
    }


    float volume =
        volumePercent / 100.0f;


    float pitch =
        (float) modules.getSlider(
            scriptName,
            "Critical Pitch"
        );


    client.playSound(
        getSound(soundIndex),
        volume,
        pitch
    );
}

void playHurtSound() {

    if (!modules.getButton(
        scriptName,
        "Hurt"
    )) {
        return;
    }


    int soundIndex =
        (int) modules.getSlider(
            scriptName,
            "Hurt Sound"
        );


    // None
    if (soundIndex == 0) {
        return;
    }


    int volumePercent =
        (int) modules.getSlider(
            scriptName,
            "Hurt Volume"
        );


    if (volumePercent <= 0) {
        return;
    }


    float volume =
        volumePercent / 100.0f;


    float pitch =
        (float) modules.getSlider(
            scriptName,
            "Hurt Pitch"
        );


    client.playSound(
        getSound(soundIndex),
        volume,
        pitch
    );
}

void playBlockSound() {

    if (!modules.getButton(
        scriptName,
        "Block"
    )) {
        return;
    }


    int soundIndex =
        (int) modules.getSlider(
            scriptName,
            "Block Sound"
        );


    // None
    if (soundIndex == 0) {
        return;
    }


    int volumePercent =
        (int) modules.getSlider(
            scriptName,
            "Block Volume"
        );


    if (volumePercent <= 0) {
        return;
    }


    float volume =
        volumePercent / 100.0f;


    float pitch =
        (float) modules.getSlider(
            scriptName,
            "Block Pitch"
        );


    client.playSound(
        getSound(soundIndex),
        volume,
        pitch
    );
}

String getSound(int index) {

    switch (index) {

        case 1:
            return "combatsounds:neverlose";

        case 2:
            return "combatsounds:fatality";

        case 3:
            return "combatsounds:gamesense";

        case 4:
            return "combatsounds:hitmarker";

        case 5:
            return "combatsounds:primordial";

        case 6:
            return "combatsounds:rust_headshot";

        case 7:
            return "random.anvil_land";

        case 8:
            return "random.orb";

        case 9:
            return "random.click";

        case 10:
            return "random.bow";

        case 11:
            return "random.pop";

        case 12:
            return "random.splash";

        case 13:
            return "fire.ignite";

        case 14:
            return "note.pling";

        case 15:
            return "random.glass";

        case 16:
            return "mob.blaze.hit";

        case 17:
            return "mob.endermen.portal";

        default:
            return "";
    }
}
