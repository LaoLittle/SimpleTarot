package org.laolittle.plugin

import kotlinx.coroutines.delay
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.info

object Tarot : KotlinPlugin(
    JvmPluginDescription(
        id = "org.laolittle.plugin.Tarot",
        name = "Tarot",
        version = "1.0-SNAPSHOT",
    ) {
        author("LaoLittle")
    }
) {

    override fun onEnable() {
        TarotData.reload()
        logger.info { "Plugin loaded" }

        GlobalEventChannel.subscribeGroupMessages {
            finding(Regex("^(?:(.+)张|)塔罗牌")) {
                val tarotNum = when (val value = it.groups[1]?.value) {
                    "一", "壹", "单", null -> 1
                    "二", "贰", "两" -> 2
                    "三", "叁", "仨" -> 3
                    "四", "肆" -> 4
                    "五", "伍" -> 5
                    "六", "陆" -> 6
                    "七", "柒" -> 7
                    "八", "捌" -> 8
                    "九", "玖" -> 9
                    "十", "拾" -> 10
                    else -> value.toIntOrNull() ?: return@finding
                }
                if (tarotNum in 1..10) {
                    val msg: (TarotData.Tarot) -> Message = { tarot ->
                        At(sender).plus(
                            PlainText(
                                "\n" +
                                        """
                            ${tarot.name}
                            ${if ((0..1).random() == 0) "正位\n${tarot.positive}" else "逆位\n${tarot.negative}"}
                        """.trimIndent()
                            )
                        )
                    }
                    val img: suspend (TarotData.Tarot) -> Image = { tarot ->
                        dataFolder.resolve(tarot.imageName).uploadAsImage(subject)
                    }
                    if (tarotNum == 1) {
                        val card = TarotData.tarot.random()
                        subject.sendMessage(msg(card))
                        delay(500)
                        subject.sendMessage(img(card))
                    } else {
                        val tarots = mutableSetOf<TarotData.Tarot>()
                        while (tarots.size < tarotNum) {
                            tarots.add(TarotData.tarot.random())
                            if (tarots.size == TarotData.tarot.size) break
                        }
                        val forward = buildForwardMessage {
                            tarots.forEach { tarot ->
                                add(sender, msg(tarot))
                                add(sender, img(tarot))
                            }
                        }
                        subject.sendMessage(forward)
                    }
                }
            }
        }
    }
}