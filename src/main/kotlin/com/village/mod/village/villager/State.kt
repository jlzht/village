package com.village.mod.village.villager

// IDLE, SIT, SWIM, SLEEP, RIDE, NONE // BODY ACTION
// ATTACK, DEFEND, FLEE, CAST, TRADE, TALK // INVENTORY ACTION
// ALERT, WORK,

enum class State {
    SLEEP,
    CAST,
    SIT,
    IDLE,
    SWIM,
    RIDE,
    NONE
}

enum class Task {
    NONE,
    TALK,
    TRAVEL,
    WORK,
    ALERT,
    FLEE,
    ATTACK,
    DEFEND
}

enum class Action {
    SLEEP,
    PLANT,
    FORGE,
    GRIND,
    STORE,
    NONE,
    TALK,
    PASS,
    MOVE,
    TILL,
    BREW,
    BAKE,
    POUR,
    FISH,
    SIT
}
