package me.kuku.telegram.extension

import com.oracle.bmc.Region
import com.oracle.bmc.core.model.CreatePublicIpDetails
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.model.PortRange
import com.oracle.bmc.model.BmcException
import kotlinx.coroutines.delay
import me.kuku.telegram.entity.OciEntity
import me.kuku.telegram.entity.OciService
import me.kuku.telegram.logic.OciLogic
import me.kuku.telegram.utils.AbilitySubscriber
import me.kuku.telegram.utils.TelegramSubscribe
import me.kuku.telegram.utils.inlineKeyboardButton
import org.ehcache.CacheManager
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Service
class OciExtension(
    private val ociService: OciService,
    cacheManager: CacheManager
) {

    fun AbilitySubscriber.oci() {

        sub("oci", "oracle cloud管理") {
            val add = inlineKeyboardButton("新增api密钥", "addOci")
            val query = inlineKeyboardButton("查询及操作", "queryOci")
            val delete = inlineKeyboardButton("删除", "deleteOci")
            val markup = InlineKeyboardMarkup(listOf(
                listOf(add),
                listOf(query),
                listOf(delete)
            ))
            sendMessage("""
                oci管理，如何创建api密钥
                租户id: 右上角头像->租户设置->租户信息->ocid
                用户id: 右上角头像->用户设置->用户信息->ocid
                其他信息：右上角头像->用户设置->api密钥->添加API密钥
            """.trimIndent(), markup)
        }

    }

    private val bindCache = cacheManager.getCache("bindOci", Long::class.javaObjectType, OciEntity::class.java)

    private val selectCache = cacheManager.getCache("selectOci", Long::class.javaObjectType, OciCache::class.java)

    fun TelegramSubscribe.oci() {
        callback("addOci") {
            editMessageText("请发送租户id")
            val tenantId = nextMessage().text
            editMessageText("请发送用户id")
            val userid = nextMessage().text
            editMessageText("请发送api密钥配置文件中的fingerprint")
            val fingerprint = nextMessage().text
            editMessageText("请发送创建api密钥下载的私钥信息，请复制全部内容并发送")
            val privateKey = nextMessage().text
            editMessageText("请发送您这个api信息要显示的名字，即备注")
            val remark = nextMessage(errMessage = "您发送的备注重复，请重新发送") {
                ociService.checkRemark(text, tgId)
            }.text
            val regions = Region.values()
            val regionList = mutableListOf<List<InlineKeyboardButton>>()
            for (i in regions.indices step 2) {
                val buttons = mutableListOf(
                    inlineKeyboardButton(regions[i].regionId, "ociRegion-${regions[i].regionId}")
                )
                if (i + 1 < regions.size) {
                    buttons.add(inlineKeyboardButton(regions[i + 1].regionId, "ociRegion-${regions[i + 1].regionId}"))
                }
                regionList.add(buttons)
            }
            bindCache.put(tgId, OciEntity().also {
                it.tenantId = tenantId
                it.userid = userid
                it.fingerprint = fingerprint
                it.privateKey = privateKey
                it.tgId = tgId
                it.remark = remark
            })
            editMessageText("请选择账号所属的区域", InlineKeyboardMarkup(regionList), top = true)
        }

        callbackStartsWith("ociRegion-") {
            val ociEntity = bindCache[tgId] ?: error("缓存已失效，请重新新增oci的api信息")
            val regionId = query.data.substring(10)
            ociEntity.region = regionId
            OciLogic.listCompartments(ociEntity)
            ociService.save(ociEntity)
            editMessageText("保存oci的api信息成功")
        }

        callback("deleteOci") {
            val delete1 = inlineKeyboardButton("删除api密钥", "deleteOciKey")
            val delete2 = inlineKeyboardButton("删除创建实例自动任务", "deleteOciCreateInstanceTask")
            editMessageText("请选择删除方式，删除api密钥也会删除该密钥下面所有的自动任务", InlineKeyboardMarkup(listOf(
                listOf(delete1),
                listOf(delete2)
            )))
        }

        callback("deleteOciKey") {
            val ociList = ociService.findByTgId(tgId)
            val buttonList = mutableListOf<List<InlineKeyboardButton>>()
            for (ociEntity in ociList) {
                buttonList.add(listOf(inlineKeyboardButton(ociEntity.remark, "ociDelete-${ociEntity.id}")))
            }
            editMessageText("请选择你需要删除的oci的key", InlineKeyboardMarkup(buttonList))
        }

        callbackStartsWith("ociDelete-") {
            val id = query.data.split("-")[1]
            ociService.deleteById(id)
            editMessageText("删除oci的api信息成功")
        }

        callback("queryOci") {
            val ociList = ociService.findByTgId(tgId)
            val buttonList = mutableListOf<List<InlineKeyboardButton>>()
            for (ociEntity in ociList) {
                buttonList.add(listOf(inlineKeyboardButton(ociEntity.remark, "ociQuery-${ociEntity.id}")))
            }
            editMessageText("请选择要操作的条目", InlineKeyboardMarkup(buttonList))
        }

        callbackStartsWith("ociQuery-") {
            val id = query.data.split("-")[1]
            val ociEntity = ociService.findById(id) ?: error("选歪了")
            selectCache.put(tgId, OciCache(ociEntity))
            val createInstance = inlineKeyboardButton("创建实例", "ociCom")
            val lookInstance = inlineKeyboardButton("查看实例", "ociLook")
            val changeRemark = inlineKeyboardButton("修改备注", "ociChRemark-$id")
            editMessageText("请选择操作方式", InlineKeyboardMarkup(listOf(
                listOf(createInstance),
                listOf(lookInstance),
                listOf(changeRemark)
            )))
        }

        callbackStartsWith("ociChRemark-") {
            val id = query.data.substring(12)
            val ociEntity = ociService.findById(id)!!
            editMessageText("请发送你修改后的备注")
            val remark = nextMessage(errMessage = "您发送的备注重复，请重新发送") {
                ociService.checkRemark(text, tgId)
            }.text
            ociEntity.remark = remark
            ociService.save(ociEntity)
            editMessageText("修改备注成功")
        }

        callback("deleteOciCreateInstanceTask") {
            val entityList = ociService.findByTgId(tgId)
            val list = mutableListOf<List<InlineKeyboardButton>>()
            for (ociEntity in entityList) {
                for ((i, createInstanceCache) in ociEntity.createInstanceList.withIndex()) {
                    list.add(listOf(
                        inlineKeyboardButton("${ociEntity.remark}-${createInstanceCache.shape}-${createInstanceCache.cpu}-${createInstanceCache.memory}", "deleteOciCreateInstanceTask-${ociEntity.id}-${i}")))
                }
            }
            editMessageText("请选择删除的创建实例自动任务", InlineKeyboardMarkup(list))
        }
        callback("deleteOciCreateInstanceTask-") {
            val arr = query.data.split("-")
            val id = arr[1]
            val num = arr[2].toInt()
            val ociEntity = ociService.findById(id) ?: error("未找到id")
            ociEntity.createInstanceList.removeAt(num)
            ociService.save(ociEntity)
            editMessageText("删除定时任务成功")
        }
    }

    private val chooseCache = cacheManager.getCache("chooseOci", String::class.java, String::class.java)

    fun TelegramSubscribe.operate() {
        before { set(selectCache[tgId] ?: error("缓存不存在，请重新发送指令后选择")) }
        callback("ociCom") {
            val shape1 = inlineKeyboardButton("VM.Standard.E2.1.Micro（amd）", "ociSelShape-1")
            val shape2 = inlineKeyboardButton("VM.Standard.A1.Flex（arm）", "ociSelShape-2")
            editMessageText("请选择创建的实例形状", InlineKeyboardMarkup(listOf(listOf(shape1), listOf(shape2))))
        }
        callbackStartsWith("ociSelShape-") {
            val i = query.data.substring(12).toInt()
            val shape = if (i == 1) "VM.Standard.E2.1.Micro" else if (i == 2) "VM.Standard.A1.Flex" else error("选歪了")
            firstArg<OciCache>().createInstanceCache.shape = shape
            val os1 = inlineKeyboardButton("Oracle-Linux-9", "ociSelOs-1")
            val os2 = inlineKeyboardButton("Oracle-Linux-8", "ociSelOs-2")
            val os3 = inlineKeyboardButton("Oracle-Linux-7", "ociSelOs-3")
            val os4 = inlineKeyboardButton("Oracle-Linux-6", "ociSelOs-4")
            val os5 = inlineKeyboardButton("Canonical-Ubuntu-22.04", "ociSelOs-5")
            val os6 = inlineKeyboardButton("Canonical-Ubuntu-20.04", "ociSelOs-6")
            val os7 = inlineKeyboardButton("Canonical-Ubuntu-18.04", "ociSelOs-7")
            val os8 = inlineKeyboardButton("CentOS-8", "ociSelOs-8")
            val os9 = inlineKeyboardButton("CentOS-7", "ociSelOs-9")
            val markup = InlineKeyboardMarkup(listOf(
                listOf(os1),
                listOf(os2),
                listOf(os3),
                listOf(os4),
                listOf(os5),
                listOf(os6),
                listOf(os7),
                listOf(os8),
                listOf(os9)
            ))
            editMessageText("请选择镜像", markup)
        }
        callbackStartsWith("ociSelOs-") {
            val ociCache = firstArg<OciCache>()
            val createInstanceCache = ociCache.createInstanceCache
            val operaSystem: String
            val version: String
            when (query.data.substring(9).toInt()) {
                1 -> ("Oracle Linux" to "9").apply { operaSystem = first; version = second }
                2 -> ("Oracle Linux" to "8").apply { operaSystem = first; version = second }
                3 -> ("Oracle Linux" to "7").apply { operaSystem = first; version = second }
                4 -> ("Oracle Linux" to "6").apply { operaSystem = first; version = second }
                5 -> ("Canonical Ubuntu" to "22.04").apply { operaSystem = first; version = second }
                6 -> ("Canonical Ubuntu" to "20.04").apply { operaSystem = first; version = second }
                7 -> ("Canonical Ubuntu" to "18.04").apply { operaSystem = first; version = second }
                8 -> ("CentOS" to "8").apply { operaSystem = first; version = second }
                9 -> ("CentOS" to "7").apply { operaSystem = first; version = second }
                else -> error("未匹配的数字")
            }
            val imageList = OciLogic.listImage(ociCache.entity, operaSystem, operatingSystemVersion = version)
            val imageId = if (createInstanceCache.shape.contains("Flex")) {
                imageList.find { it.displayName.contains("aarch64") }!!.id
            } else {
                imageList.find { !it.displayName.contains("aarch64") }!!.id
            }
            editMessageText("请发送创建实例的cpu，请注意，永久免费服务器amd只有1h1g，arm合计4h24g")
            val cpu = nextMessage().text.toFloatOrNull() ?: error("您发送的不为浮点数字")
            editMessageText("请发送创建实例的内存，请注意，永久免费服务器amd只有1h1g，arm合计4h24g")
            val memory = nextMessage().text.toFloatOrNull() ?: error("您发送的不为浮点数字")
            editMessageText("请发送创建实例的磁盘，请数字，永久免费配额只有200G的磁盘，所以磁盘因在50G和200G之间")
            val volumeSize = nextMessage().text.toLongOrNull() ?: error("您发送的不为数字")
            editMessageText("请发送创建实例的root密码")
            val password = nextMessage().text
            val instance = try {
                OciLogic.launchInstance(
                    ociCache.entity,
                    imageId,
                    cpu,
                    memory,
                    volumeSize,
                    createInstanceCache.shape,
                    password
                )
            } catch (e: BmcException) {
                val exMessage = e.message!!
                if (exMessage.contains("Out of host capacity")) {
                    createInstanceCache.imageId = imageId
                    createInstanceCache.cpu = cpu
                    createInstanceCache.memory = memory
                    createInstanceCache.volumeSize = volumeSize
                    createInstanceCache.rootPassword = password
                    val button1 = inlineKeyboardButton("添加", "ociAddTask-1")
                    val button2 = inlineKeyboardButton("取消", "ociAddTask-0")
                    editMessageText("没有货了，创建实例失败，你可以把该任务添加到自动任务中，以自动创建，是否添加", InlineKeyboardMarkup(listOf(
                        listOf(button1),
                        listOf(button2)
                    )))
                } else {
                    editMessageText("创建实例失败，异常信息：${e.message}")
                }
                return@callbackStartsWith
            }
            editMessageText("创建实例成功，查询ip中")
            delay(1000 * 15)
            val attachment = OciLogic.oneVnicAttachmentsByInstanceId(ociCache.entity, instance.id)
            val vnic = OciLogic.getVnic(ociCache.entity, attachment.vnicId)
            val publicIp = vnic.publicIp
            editMessageText("""
                创建实例成功
                ip：$publicIp
            """.trimIndent())
        }
        callbackStartsWith("ociAddTask-") {
            val i = query.data.substring(11).toIntOrNull() ?: error("错误的代码")
            if (i == 1) {
                val ociCache = firstArg<OciCache>()
                val createInstanceCache = ociCache.createInstanceCache
                val entity = ociCache.entity
                entity.createInstanceList.add(createInstanceCache)
                ociService.save(entity)
                editMessageText("添加任务成功")
            } else {
                editMessageText("已取消")
            }
        }
        callback("ociLook") {
            val entity = firstArg<OciCache>().entity
            val instances = OciLogic.listInstances(entity).filter {
                it.lifecycleState !in listOf(
                    Instance.LifecycleState.Terminated,
                    Instance.LifecycleState.Terminating
                )
            }
            val list = mutableListOf<List<InlineKeyboardButton>>()
            for ((i, instance) in instances.withIndex()) {
                val shapeConfig = instance.shapeConfig
                val title =
                    "${instance.shape}-${shapeConfig.ocpus}H-${shapeConfig.memoryInGBs}G-${shapeConfig.networkingBandwidthInGbps}G"
                list.add(listOf(inlineKeyboardButton(title, "ociLookDetail-$i")))
                chooseCache.put("$tgId$i", instance.id)
            }
            editMessageText("请选择实例，其显示内容为：\n形状-cpu核心数量-内存-带宽", InlineKeyboardMarkup(list))
        }

    }

    fun TelegramSubscribe.operateInstance() {
        before {
            set(selectCache[tgId] ?: error("缓存不存在，请重新发送指令后选择"))
            val int = query.data.split("-")[1].toInt()
            set(int)
            set(chooseCache["$tgId$int"] ?: error("缓存不存在，请重新发送指令"))
        }
        callbackStartsWith("ociLookDetail-") {
            val instanceId = thirdArg<String>()
            val ociEntity = firstArg<OciCache>().entity
            val i = secondArg<Int>()
            val instance = OciLogic.getInstance(ociEntity, instanceId)
            val vnic = OciLogic.vnicByInstance(ociEntity, instance)
            val listBootVolumeAttachments = OciLogic.listBootVolumeAttachments(ociEntity, instanceId)
            val bootVolumeAttachment = listBootVolumeAttachments[0]
            val bootVolumeId = bootVolumeAttachment.bootVolumeId
            val bootVolume = OciLogic.getBootVolume(ociEntity, bootVolumeId)
            val shapeConfig = instance.shapeConfig
            val start = inlineKeyboardButton("启动", "ociOpStart-$i")
            val stop = inlineKeyboardButton("停止", "ociOpStop-$i")
            val terminate = inlineKeyboardButton("销毁", "ociOpTerminate-$i")
            val updateIp = inlineKeyboardButton("更换ip", "ociOpUpdateIp-$i")
            val securityList = inlineKeyboardButton("安全列表（入）", "ociQuerySecList-$i")
            editMessageText("""
                请选择您的操作，您的该实例信息为：
                形状：${instance.shape}
                cpu：${shapeConfig.ocpus}H/C
                内存：${shapeConfig.memoryInGBs}G
                带宽；${shapeConfig.networkingBandwidthInGbps}G
                硬盘：${bootVolume.sizeInGBs}G
                公网IP：${vnic.publicIp}
                内网IP：${vnic.privateIp}
            """.trimIndent(), InlineKeyboardMarkup(listOf(
                listOf(start, stop, terminate),
                listOf(updateIp),
                listOf(securityList)
            )))
        }
        callbackStartsWith("ociOpStart-") {
            val ociEntity = firstArg<OciCache>().entity
            val instanceId = thirdArg<String>()
            OciLogic.instanceAction(ociEntity, instanceId, "START")
            editMessageText("启动实例成功")
        }
        callbackStartsWith("ociOpStop-") {
            val ociEntity = firstArg<OciCache>().entity
            val instanceId = thirdArg<String>()
            OciLogic.instanceAction(ociEntity, instanceId, "STOP")
            editMessageText("停止实例成功")
        }
        callbackStartsWith("ociOpTerminate-") {
            val ociEntity = firstArg<OciCache>().entity
            val instanceId = thirdArg<String>()
            OciLogic.terminateInstance(ociEntity, instanceId, true)
            editMessageText("销毁实例成功", top = true)
        }
        callbackStartsWith("ociOpUpdateIp-") {
            val ociEntity = firstArg<OciCache>().entity
            val instanceId = thirdArg<String>()
            val instance = OciLogic.getInstance(ociEntity, instanceId)
            val vnic = OciLogic.vnicByInstance(ociEntity, instance)
            val publicIp = vnic.publicIp
            val privateIpList = OciLogic.listPrivateIps(ociEntity, vnicId = vnic.id)
            if (publicIp != null) {
                val ip = OciLogic.getPublicIpByIpAddress(ociEntity, publicIp)
                OciLogic.deletePublicIp(ociEntity, ip.id)
            }
            val createPublicIp =
                OciLogic.createPublicIp(ociEntity, CreatePublicIpDetails.Lifetime.Ephemeral, privateIpList[0].id)
            val ipAddress = createPublicIp.ipAddress
            editMessageText("更新ip成功，您的新ip为：$ipAddress", refreshReturn = true)
        }
        callbackStartsWith("ociQuerySecList-") {
            val ociEntity = firstArg<OciCache>().entity
            val instanceId = thirdArg<String>()
            val attachment = OciLogic.oneVnicAttachmentsByInstanceId(ociEntity, instanceId)
            val subnetId = attachment.subnetId
            val subnet = OciLogic.getSubnet(ociEntity, subnetId)
            val vcnId = subnet.vcnId
            val securityList = OciLogic.listSecurityLists(ociEntity, vcnId)
            val ingressSecurityRules = securityList[0].ingressSecurityRules
            val buttonList = mutableListOf<List<InlineKeyboardButton>>()
            for (rule in ingressSecurityRules) {
                val protocol = rule.protocol.toInt()
                val type = when (protocol) {
                    1 -> "icmp"
                    6 -> "tcp"
                    17 -> "udp"
                    58 -> "icmpV6"
                    else -> "unknown"
                }
                var text = "${rule.source} $type"
                if (protocol in listOf(6, 17)) {
                    var sourcePortRange: PortRange? = null
                    var destinationPortRange: PortRange? = null
                    rule.tcpOptions?.let {
                        sourcePortRange = it.sourcePortRange
                        destinationPortRange = it.destinationPortRange
                    }
                    rule.udpOptions?.let {
                        sourcePortRange = it.sourcePortRange
                        destinationPortRange = it.destinationPortRange
                    }
                    val source = if (sourcePortRange == null) "全部" else "${sourcePortRange!!.min}-${sourcePortRange!!.max}"
                    val destination = if (destinationPortRange == null) "全部" else "${destinationPortRange!!.min}-${destinationPortRange!!.max}"
                    text += " $source $destination"
                }
                buttonList.add(listOf(inlineKeyboardButton(text, "notWrite")))
            }
            buttonList.add(listOf(inlineKeyboardButton("新增", "notWrite")))
            editMessageText("您的该实例的安全列表如下：\n格式：源 协议 源端口范围 目的地端口范围\n您可以点击安全列表按钮来进行删除",
                InlineKeyboardMarkup(buttonList))
        }
    }

}

data class OciCache(
    val entity: OciEntity,
    val createInstanceCache: CreateInstanceCache = CreateInstanceCache()
)

class CreateInstanceCache {
    var shape: String = ""
    var imageId: String = ""
    var cpu: Float = 0f
    var memory: Float = 0f
    var volumeSize: Long = 0
    var rootPassword: String = ""
}
