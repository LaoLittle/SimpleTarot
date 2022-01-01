package org.laolittle.plugin

import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

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

    companion object {
        private fun File.rotateImage(): ExternalResource {
            val buffImage = ImageIO.read(this)
            val w = buffImage.width
            val h = buffImage.height
            val type = buffImage.colorModel.transparency
            val img = BufferedImage(w, h, type)
            val g2: Graphics2D = img.createGraphics()
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2.rotate(Math.toRadians(180.0), (w shr 1).toDouble(), (h shr 1).toDouble())
            g2.drawImage(buffImage, 0, 0, null)
            g2.dispose()
            val bytes = ByteArrayOutputStream()
            ImageIO.write(img, "jpg", bytes)
            return bytes.toByteArray().toExternalResource()
        }
    }
}