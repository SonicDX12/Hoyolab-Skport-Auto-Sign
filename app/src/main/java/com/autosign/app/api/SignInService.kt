package com.autosign.app.api

import com.autosign.app.data.model.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class SignResult(val accountNickname: String, val lines: List<Pair<String, Boolean>>)

class SignInService {

    private val client = OkHttpClient()

    private val hoYoBaseHeaders = mapOf(
        "Accept" to "application/json, text/plain, */*",
        "Accept-Encoding" to "gzip, deflate, br",
        "Connection" to "keep-alive",
        "x-rpc-app_version" to "2.34.1",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36",
        "x-rpc-client_type" to "4",
        "Referer" to "https://act.hoyolab.com/",
        "Origin" to "https://act.hoyolab.com"
    )

    private val hoYoUrls = mapOf(
        "Genshin Impact" to "https://sg-hk4e-api.hoyolab.com/event/sol/sign?lang=en-us&act_id=e202102251931481",
        "Honkai: Star Rail" to "https://sg-public-api.hoyolab.com/event/luna/os/sign?lang=en-us&act_id=e202303301540311",
        "Honkai Impact 3rd" to "https://sg-public-api.hoyolab.com/event/mani/sign?lang=en-us&act_id=e202110291205111",
        "Tears of Themis" to "https://sg-public-api.hoyolab.com/event/luna/os/sign?lang=en-us&act_id=e202308141137581",
        "Zenless Zone Zero" to "https://sg-public-api.hoyolab.com/event/luna/zzz/os/sign?lang=en-us&act_id=e202406031448091"
    )

    suspend fun signIn(account: Account): SignResult = withContext(Dispatchers.IO) {
        when (account) {
            is Account.HoYoLab -> signInHoYoLab(account)
            is Account.SkPort -> signInSkPort(account)
        }
    }

    private suspend fun signInHoYoLab(account: Account.HoYoLab): SignResult {
        val targets = buildList {
            if (account.genshin) add("Genshin Impact")
            if (account.honkaiStarRail) add("Honkai: Star Rail")
            if (account.honkai3) add("Honkai Impact 3rd")
            if (account.tearsOfThemis) add("Tears of Themis")
            if (account.zenlessZoneZero) add("Zenless Zone Zero")
        }

        val lines = mutableListOf<Pair<String, Boolean>>()
        targets.forEachIndexed { index, game ->
            if (index > 0) delay(1000L)
            val url = hoYoUrls[game] ?: return@forEachIndexed
            val headers = buildMap {
                putAll(hoYoBaseHeaders)
                put("Cookie", account.token)
                if (game == "Zenless Zone Zero") put("x-rpc-signgame", "zzz")
            }
            val req = Request.Builder().url(url)
                .post("{}".toRequestBody("application/json".toMediaType()))
                .apply { headers.forEach { (k, v) -> header(k, v) } }
                .build()

            try {
                val resp = client.newCall(req).execute()
                val body = resp.body?.string() ?: "{}"
                val responseJson = JSONObject(body)
                val message = responseJson.optString("message", "Unknown error")
                val isCaptcha = responseJson.optJSONObject("data")
                    ?.optJSONObject("gt_result")?.optBoolean("is_risk", false) ?: false
                when {
                    isCaptcha -> lines.add("$game: Blocked by CAPTCHA" to false)
                    message == "OK" -> lines.add("$game: Check-in successful" to true)
                    else -> lines.add("$game: $message" to false)
                }
            } catch (e: Exception) {
                lines.add("$game: Network error — ${e.message}" to false)
            }
        }

        if (targets.isEmpty()) lines.add("No games enabled" to false)
        return SignResult(account.nickname, lines)
    }

    private fun generateSkPortSign(path: String, body: String, timestamp: String, key: String): String {
        val headerObj = """{"platform":"3","timestamp":"$timestamp","dId":"","vName":"1.0.0"}"""
        val stringToSign = path + body + timestamp + headerObj

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        val hmacBytes = mac.doFinal(stringToSign.toByteArray())
        val hmacHex = hmacBytes.joinToString("") { "%02x".format(it) }

        val md5 = MessageDigest.getInstance("MD5")
        // MD5 is applied to the hex STRING's bytes, not the raw HMAC bytes — matches server-side signing exactly.
        val md5Bytes = md5.digest(hmacHex.toByteArray())
        return md5Bytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateSignV1(timestamp: String, cred: String): String {
        val digest = MessageDigest.getInstance("MD5").digest("timestamp=$timestamp&cred=$cred".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private data class SkPortAuth(val cred: String, val salt: String, val userId: String)

    // Marks any auth-chain failure caused by a dead/invalid account token, so signInSkPort can
    // show one consistent "please reconnect" message instead of a different technical string
    // per failure point (token validation vs. OAuth grant vs. credential generation).
    private class InvalidTokenException(message: String) : Exception(message)

    private fun performOAuthFlow(accountToken: String): SkPortAuth {
        val infoUrl = "https://as.gryphline.com/user/info/v1/basic?token=${URLEncoder.encode(accountToken, "UTF-8")}"
        val infoJson = JSONObject(client.newCall(Request.Builder().url(infoUrl).build()).execute().body?.string() ?: "{}")
        if (infoJson.optInt("status", -1) != 0) throw InvalidTokenException("token validation failed")

        val grantPayload = JSONObject().put("token", accountToken).put("appCode", "6eb76d4e13aa36e6").put("type", 0).toString()
        val grantReq = Request.Builder().url("https://as.gryphline.com/user/oauth2/v2/grant")
            .post(grantPayload.toRequestBody("application/json".toMediaType())).build()
        val grantJson = JSONObject(client.newCall(grantReq).execute().body?.string() ?: "{}")
        val code = grantJson.optJSONObject("data")?.optString("code")
        if (grantJson.optInt("status", -1) != 0 || code.isNullOrEmpty()) throw InvalidTokenException("OAuth grant failed")

        val credPayload = JSONObject().put("code", code).put("kind", 1).toString()
        val credReq = Request.Builder().url("https://zonai.skport.com/web/v1/user/auth/generate_cred_by_code")
            .header("platform", "3")
            .post(credPayload.toRequestBody("application/json".toMediaType())).build()
        val credJson = JSONObject(client.newCall(credReq).execute().body?.string() ?: "{}")
        val credData = credJson.optJSONObject("data")
        val cred = credData?.optString("cred")
        if (credJson.optInt("code", -1) != 0 || cred.isNullOrEmpty()) throw InvalidTokenException("credential generation failed")

        return SkPortAuth(cred = cred, salt = credData.optString("token"), userId = credData.optString("userId", ""))
    }

    private fun fetchSkGameRole(cred: String, salt: String): String? {
        val path = "/api/v1/game/player/binding"
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val sign = generateSkPortSign(path, "", timestamp, salt)
        val headers = mapOf(
            "cred" to cred, "platform" to "3", "sk-language" to "en", "timestamp" to timestamp,
            "vname" to "1.0.0", "sign" to sign,
            "User-Agent" to "Skport/0.7.0 (com.gryphline.skport; build:700089; Android 33; ) Okhttp/5.1.0",
            "Origin" to "https://game.skport.com", "Referer" to "https://game.skport.com/"
        )
        val req = Request.Builder().url("https://zonai.skport.com$path")
            .apply { headers.forEach { (k, v) -> header(k, v) } }.build()
        val json = JSONObject(client.newCall(req).execute().body?.string() ?: "{}")
        if (json.optInt("code", -1) != 0) return null

        val apps = json.optJSONObject("data")?.optJSONArray("list") ?: return null
        for (i in 0 until apps.length()) {
            val app = apps.getJSONObject(i)
            if (app.optString("appCode") != "endfield") continue
            val bindingList = app.optJSONArray("bindingList") ?: continue
            if (bindingList.length() == 0) continue
            val binding = bindingList.getJSONObject(0)
            val role = binding.optJSONObject("defaultRole")
                ?: binding.optJSONArray("roles")?.takeIf { it.length() > 0 }?.getJSONObject(0)
                ?: continue
            return "3_${role.getString("roleId")}_${role.getString("serverId")}"
        }
        return null
    }

    private fun signInSkPort(account: Account.SkPort): SignResult {
        val reconnect = SignResult(account.nickname, listOf("Endfield: Please reconnect your account" to false))
        return try {
            val (cred, salt, _) = performOAuthFlow(account.accountToken)
            val skGameRole = fetchSkGameRole(cred, salt)
                ?: return SignResult(account.nickname, listOf(
                    "Endfield: Could not detect game role — check that your account is bound to Arknights: Endfield" to false))

            val path = "/web/v1/game/endfield/attendance"
            val url = "https://zonai.skport.com$path"
            fun buildHeaders(sign: String, timestamp: String) = mapOf(
                "cred" to cred, "sk-game-role" to skGameRole, "platform" to "3",
                "sk-language" to account.language, "timestamp" to timestamp, "vname" to "1.0.0", "sign" to sign,
                "User-Agent" to "Skport/0.7.0 (com.gryphline.skport; build:700089; Android 33; ) Okhttp/5.1.0",
                "Origin" to "https://game.skport.com", "Referer" to "https://game.skport.com/"
            )

            // Status check first: claiming an already-claimed day errors server-side, so hasToday
            // must short-circuit before any POST is attempted.
            var timestamp = (System.currentTimeMillis() / 1000).toString()
            var sign = generateSkPortSign(path, "", timestamp, salt)
            val statusReq = Request.Builder().url(url).apply { buildHeaders(sign, timestamp).forEach { (k, v) -> header(k, v) } }.build()
            val statusJson = JSONObject(client.newCall(statusReq).execute().body?.string() ?: "{}")

            if (statusJson.optInt("code", -1) == 10000) return reconnect
            if (statusJson.optInt("code", -1) != 0) {
                return SignResult(account.nickname, listOf("Endfield: ${statusJson.optString("message", "Unknown error")}" to false))
            }
            if (statusJson.optJSONObject("data")?.optBoolean("hasToday", false) == true) {
                return SignResult(account.nickname, listOf("Endfield: Already signed in today" to true))
            }

            // Timestamp/sign are regenerated for the claim POST, not reused from the status GET.
            timestamp = (System.currentTimeMillis() / 1000).toString()
            sign = generateSkPortSign(path, "", timestamp, salt)
            val claimReq = Request.Builder().url(url)
                .post("".toRequestBody("application/json".toMediaType()))
                .apply { buildHeaders(sign, timestamp).forEach { (k, v) -> header(k, v) } }
                .build()
            val claimJson = JSONObject(client.newCall(claimReq).execute().body?.string() ?: "{}")

            if (claimJson.optInt("code", -1) == 10000) return reconnect
            if (claimJson.optInt("code", -1) != 0) {
                return SignResult(account.nickname, listOf("Endfield: ${claimJson.optString("message", "Unknown error")}" to false))
            }

            val data = claimJson.optJSONObject("data")
            val awardIds = data?.optJSONArray("awardIds")
            val resourceMap = data?.optJSONObject("resourceInfoMap")
            val rewardLine = if (awardIds != null && resourceMap != null && awardIds.length() > 0) {
                (0 until awardIds.length()).mapNotNull { i ->
                    val id = awardIds.optString(i)
                    val info = resourceMap.optJSONObject(id) ?: return@mapNotNull null
                    "${info.optString("name", id)} x${info.optInt("count", 1)}"
                }.joinToString(", ")
            } else ""

            val line = if (rewardLine.isNotBlank()) "Endfield: $rewardLine" else "Endfield: Check-in successful"
            SignResult(account.nickname, listOf(line to true))
        } catch (e: InvalidTokenException) {
            reconnect
        } catch (e: Exception) {
            SignResult(account.nickname, listOf("Endfield: Network error — ${e.message}" to false))
        }
    }

    suspend fun sendDiscordWebhook(webhook: String, discordUserId: String = "", accounts: List<Account>, results: List<SignResult>) = withContext(Dispatchers.IO) {
        if (webhook.isBlank()) return@withContext
        val ping = if (discordUserId.isNotBlank()) "<@$discordUserId> " else ""
        accounts.zip(results).forEachIndexed { index, (account, result) ->
            if (index > 0) delay(350L) // stay well under Discord's per-webhook rate limit
            // The username is a fixed per-provider label now, not the account nickname, so the
            // nickname has to stay in the content to tell accounts of the same provider apart.
            val content = "**${account.nickname}**\n" + result.lines.joinToString("\n") { (line, ok) ->
                "${if (ok) "✅" else "⚠️"} ${if (!ok) ping else ""}$line"
            }
            val username = when (account) {
                is Account.HoYoLab -> "HoYoLab-Auto-Sign"
                is Account.SkPort -> "SkPort-Auto-Sign"
            }
            val payload = JSONObject().apply {
                put("username", username)
                if (account.discordAvatarUrl.isNotBlank()) put("avatar_url", account.discordAvatarUrl)
                put("content", content)
            }.toString()
            val req = Request.Builder().url(webhook)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            try { client.newCall(req).execute() } catch (_: Exception) {}
        }
    }
}
