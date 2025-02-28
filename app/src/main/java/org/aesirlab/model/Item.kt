package org.aesirlab.model

import com.zybooks.sksolidannotations.SolidAnnotation


@SolidAnnotation("https://example.org", "itemThing")
data class Item(var id: String, var name: String, var amount: Int = 0)


