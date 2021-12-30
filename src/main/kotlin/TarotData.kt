package org.laolittle.plugin

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object TarotData : AutoSavePluginData("TarotData") {
    @Serializable
    data class Tarot(
        val name: String,
        val positive: String,
        val negative: String,
        val imageName: String
    )

    val tarot by value(arrayOf(Tarot("塔罗牌名", "正位", "逆位", "图片名称")))
}