package com.panosdim.flatman.model

data class Flat(var id: Int? = null, var name: String, var address: String, var floor: Int) {
    override fun toString(): String {
        return name
    }
}