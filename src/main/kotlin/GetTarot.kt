package org.laolittle.plugin

import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.laolittle.plugin.Tools.rotateImage
import java.io.File

class GetTarot(
    private val tarot: TarotData.Tarot,
    private val user: User
) {
    private val path: File = SimpleTarot.dataFolder.resolve("TarotImages")
    private val random = (0..1).random()

    val tarotMessage: Message
        get() {
            return TarotConfig.format.replace("%目标%", "[mirai:at:${user.id}]")
                .replace("%牌名%", tarot.name).replace(
                    "%描述%",
                    if (random == 0) "正位\n${tarot.positive}" else "逆位\n${tarot.negative}"
                ).deserializeMiraiCode()
        }
    val tarotImage: ExternalResource?
        get() {
            val imgFile = path.resolve(tarot.imageName)
            return if (imgFile.exists()) {
                if (random == 0 || !TarotConfig.rotate)
                    imgFile.toExternalResource()
                else
                    imgFile.rotateImage()
            } else null
        }
}