package org.laolittle.plugin

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object TarotConfig : AutoSavePluginConfig("Config") {
    @ValueDescription("用户数据库文件名")
    val database by value("user.sqlite")

    @ValueDescription("塔罗牌图片发送间隔 (单位: 毫秒)")
    val interval by value(500L)

    @ValueDescription("塔罗牌发送格式")
    val format by value("%目标%\n%牌名%\n%描述%")

    @ValueDescription("连抽可重复")
    val repeatable by value(true)
}