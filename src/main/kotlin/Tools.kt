package org.laolittle.plugin

import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

object Tools {
    fun File.rotateImage(): ExternalResource {
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