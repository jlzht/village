package com.village.mod.village.structure

import com.village.mod.entity.village.Errand

class StructureData {
    val structure: StructureType
    val capacity: Int
    val errands: HashSet<Errand>

    constructor(structure: StructureType, capacity: Int, errands: HashSet<Errand>) {
        this.structure = structure
        this.capacity = capacity
        this.errands = errands
    }
}
