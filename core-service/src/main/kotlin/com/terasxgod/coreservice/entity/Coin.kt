package com.terasxgod.coreservice.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "coin")
data class Coin(
    @Id
    var id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var symbol: String,

    @Column(nullable = true)
    var iconUrl: String? = null,

) {
}