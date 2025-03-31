package org.aesirlab.model

data class Item(val id: String, val name: String, val amount: Int = 0) {
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other !is Item) return false
//
//        if (other.amount != this.amount) return false
//        if (other.id != this.id) return false
//        if (other.name != this.name) return false
//        return true
//    }
//
//    override fun hashCode(): Int {
//        var result = id.hashCode()
//        result = 31 * result + name.hashCode()
//        result = 31 * result + amount
//        return result
//    }
}