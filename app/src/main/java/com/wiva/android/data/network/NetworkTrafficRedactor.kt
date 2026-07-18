package com.wiva.android.data.network

private val jsonSecretPatterns =
    listOf(
        Regex("""\"extApiToken\"\s*:\s*\"[^\"]*\"""", RegexOption.IGNORE_CASE),
        Regex("""\"kassaToken\"\s*:\s*\"[^\"]*\"""", RegexOption.IGNORE_CASE),
        Regex("""\"secret\"\s*:\s*\"[^\"]*\"""", RegexOption.IGNORE_CASE),
        Regex("""\"machineSecret\"\s*:\s*\"[^\"]*\"""", RegexOption.IGNORE_CASE),
        Regex("""\"registrationKey\"\s*:\s*\"[^\"]*\"""", RegexOption.IGNORE_CASE),
        Regex("""\"key\"\s*:\s*\"[^\"]*\"""", RegexOption.IGNORE_CASE),
        Regex("""\"authorization\"\s*:\s*\"[^\"]*\"""", RegexOption.IGNORE_CASE),
        Regex("""\"accessToken\"\s*:\s*\"[^\"]*\"""", RegexOption.IGNORE_CASE),
        Regex("""\"token\"\s*:\s*\"[^\"]*\"""", RegexOption.IGNORE_CASE),
    )

private val xmlSecretPatterns =
    listOf(
        Regex("""<kassatoken>[^<]*</kassatoken>""", RegexOption.IGNORE_CASE),
        Regex("""<key>[^<]*</key>""", RegexOption.IGNORE_CASE),
        Regex("""<sign>[^<]*</sign>""", RegexOption.IGNORE_CASE),
        Regex("""<secret>[^<]*</secret>""", RegexOption.IGNORE_CASE),
        Regex("""<token>[^<]*</token>""", RegexOption.IGNORE_CASE),
    )

fun redactNetworkPayload(text: String): String {
    var out = text
    jsonSecretPatterns.forEach { rx ->
        out = rx.replace(out) { m ->
            val raw = m.value
            val key = raw.substringBefore(':')
            "$key:\"***\""
        }
    }
    xmlSecretPatterns.forEach { rx ->
        out = rx.replace(out) { m ->
            val tag = m.value.substringAfter('<').substringBefore('>')
            "<$tag>***</$tag>"
        }
    }
    return out
}

fun redactHeaderValue(name: String, value: String): String {
    val lower = name.lowercase()
    return when {
        lower == "authorization" -> "***"
        lower.contains("token") -> "***"
        lower.contains("secret") -> "***"
        lower == "cookie" -> "***"
        else -> value
    }
}
