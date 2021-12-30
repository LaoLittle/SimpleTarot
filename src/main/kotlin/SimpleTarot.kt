package org.laolittle.plugin

import com.alibaba.druid.pool.DruidDataSource
import kotlinx.coroutines.delay
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.buildForwardMessage
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.info
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection
import java.time.LocalDate
import javax.sql.DataSource

object SimpleTarot : KotlinPlugin(
    JvmPluginDescription(
        id = "org.laolittle.plugin.Tarot",
        name = "Tarot",
        version = "1.0.1",
    ) {
        author("LaoLittle")
    }
) {
    private val path: File = dataFolder.resolve("TarotImages")
    private val userDB: Database.User
    private val dataSource = DruidDataSource()
    private val db: org.jetbrains.exposed.sql.Database

    override fun onEnable() {
        TarotConfig.reload()
        TarotData.reload()
        logger.info { "Plugin loaded" }

        GlobalEventChannel.subscribeGroupMessages {
            val msg: (TarotData.Tarot, User) -> Message = { tarot, target ->
                TarotConfig.format.replace("%目标%", "[mirai:at:${target.id}]")
                    .replace("%牌名%", tarot.name).replace(
                        "%描述%",
                        if ((0..1).random() == 0) "正位\n${tarot.positive}" else "逆位\n${tarot.negative}"
                    ).deserializeMiraiCode()
            }
            val img: suspend (TarotData.Tarot, Contact) -> Image = { tarot, contact ->
                path.resolve(tarot.imageName).uploadAsImage(contact)
            }
            finding(Regex("^(?:(.+)张|)塔罗牌")) {
                val sql: SqlExpressionBuilder.() -> Op<Boolean> = { userDB.id eq sender.id }
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
                val la = Database.suspendTransaction {
                    val cardRow = userDB.select(sql).singleOrNull() ?: return@suspendTransaction null
                    val cards = cardRow[userDB.card]
                    if (cardRow[userDB.card] >= tarotNum)
                        userDB.update(sql) { info ->
                            info[card] = cards - tarotNum
                        } else {
                        if (cards == 0) subject.sendMessage("你没有抽数了")
                        else subject.sendMessage("你只有${cardRow[userDB.card]}张")
                        if (cardRow[userDB.date] == LocalDate.now().dayOfYear)
                            subject.sendMessage("你的今日塔罗貌似没有抽取呢，请发送\"今日塔罗\"来获取塔罗牌次数")
                        return@suspendTransaction false
                    }
                    true
                }
                when (la) {
                    null -> {
                        subject.sendMessage("请发送\"今日塔罗\"来获取塔罗牌次数");return@finding
                    }
                    false -> return@finding
                    true -> delay(500)
                }
                if (tarotNum in 1..10) {
                    if (tarotNum == 1) {
                        val card = TarotData.tarot.random()
                        subject.sendMessage(msg(card, sender))
                        delay(TarotConfig.interval)
                        subject.sendMessage(img(card, subject))
                    } else {
                        val tarots = getRandomTarots(tarotNum)
                        val forward = buildForwardMessage {
                            tarots.forEach { tarot ->
                                add(sender, msg(tarot, sender))
                                add(sender, img(tarot, subject))
                            }
                        }
                        subject.sendMessage(forward)
                    }
                } else {
                    subject.sendMessage("次数请限制在一到十内！")
                }
            }
            startsWith("今日塔罗") {
                val sql: SqlExpressionBuilder.() -> Op<Boolean> = { userDB.id eq sender.id }
                val dayOfYear = LocalDate.now().dayOfYear
                Database.suspendTransaction {
                    val query = userDB.select(sql)
                    val user = query.singleOrNull()
                    if (user?.get(userDB.date) == dayOfYear) {
                        subject.sendMessage("你已经拿过一张今日塔罗了哦！")
                        return@suspendTransaction
                    }
                    val random = (10..30).random()
                    if (user == null)
                        userDB.insert { usr ->
                            usr[id] = sender.id
                            usr[card] = random
                            usr[date] = dayOfYear
                        }
                    else {
                        userDB.update(sql) { info ->
                            info[card] = 1
                            info[date] = dayOfYear
                        }
                    }
                    val card = TarotData.tarot.random()
                    subject.sendMessage(msg(card, sender))
                    delay(TarotConfig.interval)
                    subject.sendMessage(img(card, subject))
                    subject.sendMessage("获得$random 张塔罗牌")
                }
            }
            startsWith("查看塔罗") {

            }
        }

    }

    private fun getRandomTarots(tarotNum: Int): Collection<TarotData.Tarot> {
        val tarots = if (TarotConfig.repeatable) arrayListOf() else mutableSetOf<TarotData.Tarot>()
            while (tarots.size < tarotNum) {
                tarots.add(TarotData.tarot.random())
                if (tarots.size == TarotData.tarot.size) break
            }
        return tarots
    }

    init {
        dataSource.url = "jdbc:sqlite:${SimpleTarot.dataFolder}/${TarotConfig.database}"
        dataSource.driverClassName = "org.sqlite.JDBC"
        db = org.jetbrains.exposed.sql.Database.connect(dataSource as DataSource)
        TransactionManager.manager.defaultIsolationLevel =
            Connection.TRANSACTION_SERIALIZABLE
        userDB = Database.User
        transaction {
            SchemaUtils.create(userDB)
        }
    }
}