package com.autosign.app.data.model

import java.net.URLDecoder

object TokenExtraction {

    fun extractHoYoLabToken(rawCookieHeader: String): String? {
        val cookies = parse(rawCookieHeader)
        if (cookies["ltoken_v2"].isNullOrBlank()) return null
        val keys = listOf("account_mid_v2", "account_id_v2", "ltoken_v2", "ltmid_v2", "ltuid_v2")
        return keys.mapNotNull { k -> cookies[k]?.let { "$k=$it;" } }.joinToString(" ")
    }

    fun extractSkPortToken(rawCookieHeader: String): String? {
        // ACCOUNT_TOKEN is httpOnly, so page JS can never read it via document.cookie — but the
        // native CookieManager cookie-jar API is not subject to that flag, which is what makes
        // this work. It's scoped to Path=/cookie_store/account_token (not "/"), so the caller
        // must query that specific URL — see TokenLoginWebView's cookieLookupUrl.
        val raw = parse(rawCookieHeader)["ACCOUNT_TOKEN"] ?: return null
        return runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrNull()
    }

    private fun parse(header: String): Map<String, String> =
        header.split(";").mapNotNull { part ->
            val kv = part.trim().split("=", limit = 2)
            if (kv.size == 2) kv[0] to kv[1] else null
        }.toMap()
}
