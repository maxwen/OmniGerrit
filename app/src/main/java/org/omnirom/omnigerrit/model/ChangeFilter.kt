package org.omnirom.omnigerrit.model

import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.omnirom.omnigerrit.utils.BuildImageUtils
import org.omnirom.omnigerrit.utils.LogUtils
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory


object ChangeFilter {
    val TAG = "ChangeFilter"

    var defaultBranch: String = ""
    val gerritDateTimeFormat by lazy {
        initDateTimeFormat()
    }
    val gerritDateFormat by lazy {
        initDateFormat()
    }
    private val hideProjectList = listOf("android_device_", "android_hardware_", "android_kernel_")

    private val deviceProjectMap = mutableMapOf<String, Map<String, Any>>()
    private val thisDeviceProjectList = mutableListOf<String>()
    val deviceMapLoaded = MutableStateFlow<Boolean>(false)

    private fun initDateTimeFormat(): SimpleDateFormat {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm")
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format
    }

    private fun initDateFormat(): SimpleDateFormat {
        val format = SimpleDateFormat("yyyy-MM-dd")
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format
    }

    enum class Status {
        Merged, Open
    }

    data class QueryFilter(
        var queryString: String = "", var queryDateAfter: String = "",
        var projectFilter: Boolean = true
    )

    fun createQueryString(
        branch: String = "",
        project: String = "",
        status: Status = Status.Merged,
        message: String = "",
        after: String = ""
    ): String {
        val q = mutableListOf<String>()
        if (branch.isNotEmpty()) {
            q.add("branch:$branch")
        } else {
            q.add("branch:$defaultBranch")
        }
        if (project.isNotEmpty()) {
            q.add("project:$project")
        }
        if (message.isNotEmpty()) {
            q.add("message:$message")
        }
        if (after.isNotEmpty()) {
            q.add("after:$after")
        }
        when (status) {
            Status.Merged -> q.add("status:merged")
            Status.Open -> q.add("status:open")
        }
        return q.joinToString(separator = "+")
    }

    fun showProject(project: String): Boolean {
        return thisDeviceProjectList.contains(project) || hideProjectList.none() { project.startsWith(it) }
    }

    private fun getOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC

        val oktHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)

        return oktHttpClient.build()
    }

    fun loadProjectFilterConfigFile() {
        if (defaultBranch.isNotEmpty()) {
            val deviceMapURL =
                "https://raw.githubusercontent.com/omnirom/android_vendor_omni/$defaultBranch/changelog/projects.xml"

            val client = getOkHttpClient()
            val request = Request.Builder().url(deviceMapURL).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.let {
                    val input = it.byteStream()
                    loadDeviceMap(input)
                }
            }
        }
        // important cause someone is waiting for this
        deviceMapLoaded.value = true
    }

    private fun loadDeviceMap(input: InputStream) {
        deviceProjectMap.clear()
        try {
            val dbf = DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            var doc = db.parse(input)
            doc.documentElement.normalize()

            val oemList = doc.documentElement.childNodes
            for (i in 0 until oemList.length) {
                if (oemList.item(i).nodeType != Node.ELEMENT_NODE) continue
                val oem = oemList.item(i) as Element
                val oemName = oem.getAttribute("name")
                val deviceList = oem.childNodes
                for (j in 0 until deviceList.length) {
                    if (deviceList.item(j).nodeType != Node.ELEMENT_NODE) continue
                    val device = deviceList.item(j) as Element
                    val properties = device.childNodes
                    val deviceMap = mutableMapOf<String, Any>()
                    var deviceName: String? = null
                    for (k in 0 until properties.length) {
                        if (properties.item(k).nodeType != Node.ELEMENT_NODE) continue
                        val property = properties.item(k) as Element
                        if (property.nodeName == "name") {
                            deviceMap["name"] = property.textContent
                        }
                        if (property.nodeName == "code") {
                            deviceName = property.textContent
                            deviceMap["code"] = deviceName
                        }
                        if (property.nodeName == "repos") {
                            val projectList = mutableListOf<String>()
                            val gitList = property.childNodes
                            for (i in 0 until gitList.length) {
                                if (gitList.item(i).nodeType != Node.ELEMENT_NODE) continue
                                projectList.add((gitList.item(i) as Element).getAttribute("name"))
                            }
                            deviceMap["repos"] = projectList
                        }
                    }
                    if (deviceName != null) {
                        deviceProjectMap.put(deviceName, deviceMap)
                    }
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "loadDeviceMap", e)
        }
        val device = BuildImageUtils.device
        if (device.isNotEmpty()) {
            if (deviceProjectMap.containsKey(device)) {
                if (deviceProjectMap[device]!!.containsKey("repos")) {
                    thisDeviceProjectList.clear()
                    thisDeviceProjectList.addAll(deviceProjectMap[device]!!["repos"] as List<String>)
                }
            }
        }
        LogUtils.d(TAG, "thisDeviceProjectList = " + thisDeviceProjectList)
    }
}